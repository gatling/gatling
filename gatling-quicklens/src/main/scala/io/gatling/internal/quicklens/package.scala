/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// the original sources from https://github.com/softwaremill/quicklens are lacking file headers.
// we'll add them as soon as they are added upstream.
package io.gatling.internal

import scala.annotation.compileTimeOnly
import scala.collection.Factory
import scala.language.experimental.macros
import scala.language.higherKinds

package object quicklens extends LowPriorityImplicits {

  private[quicklens] def canOnlyBeUsedInsideModify(method: String) =
    s"$method can only be used inside modify"

  /**
   * Create an object allowing modifying the given (deeply nested) field accessible in a `case class` hierarchy via `path` on the given `obj`.
   *
   * All modifications are side-effect free and create copies of the original objects.
   *
   * You can use `.each` to traverse options, lists, etc.
   */
  def modify[T, U](obj: T)(path: T => U): PathModify[T, U] = macro QuicklensMacros.modify_impl[T, U]

  /**
   * Create an object allowing modifying the given (deeply nested) fields accessible in a `case class` hierarchy via `paths` on the given `obj`.
   *
   * All modifications are side-effect free and create copies of the original objects.
   *
   * You can use `.each` to traverse options, lists, etc.
   */
  def modifyAll[T, U](obj: T)(path1: T => U, paths: (T => U)*): PathModify[T, U] =
    macro QuicklensMacros.modifyAll_impl[T, U]

  implicit class ModifyPimp[T](t: T) {

    /**
     * Create an object allowing modifying the given (deeply nested) field accessible in a `case class` hierarchy via `path` on the given `obj`.
     *
     * All modifications are side-effect free and create copies of the original objects.
     *
     * You can use `.each` to traverse options, lists, etc.
     */
    def modify[U](path: T => U): PathModify[T, U] = macro QuicklensMacros.modifyPimp_impl[T, U]

    /**
     * Create an object allowing modifying the given (deeply nested) fields accessible in a `case class` hierarchy via `paths` on the given `obj`.
     *
     * All modifications are side-effect free and create copies of the original objects.
     *
     * You can use `.each` to traverse options, lists, etc.
     */
    def modifyAll[U](path1: T => U, paths: (T => U)*): PathModify[T, U] = macro QuicklensMacros.modifyAllPimp_impl[T, U]
  }

  final case class PathModify[T, U](obj: T, doModify: (T, U => U) => T) {

    /**
     * Transform the value of the field(s) using the given function.
     *
     * @return
     *   A copy of the root object with the (deeply nested) field(s) modified.
     */
    def using(mod: U => U): T = doModify(obj, mod)

    /**
     * An alias for [[using]]. Explicit calls to [[using]] are preferred over this alias, but quicklens provides this option because code auto-formatters (like
     * scalafmt) will generally not keep [[modify]]/[[using]] pairs on the same line, leading to code like
     * {{{
     * x
     *   .modify(_.foo)
     *   .using(newFoo :: _)
     *   .modify(_.bar)
     *   .using(_ + newBar)
     * }}}
     * When using [[apply]], scalafmt will allow
     * {{{
     * x
     *   .modify(_.foo)(newFoo :: _)
     *   .modify(_.bar)(_ + newBar)
     * }}}
     */
    final def apply(mod: U => U): T = using(mod)

    /**
     * Transform the value of the field(s) using the given function, if the condition is true. Otherwise, returns the original object unchanged.
     *
     * @return
     *   A copy of the root object with the (deeply nested) field(s) modified, if `condition` is true.
     */
    def usingIf(condition: Boolean)(mod: U => U): T = if (condition) doModify(obj, mod) else obj

    /**
     * Set the value of the field(s) to a new value.
     *
     * @return
     *   A copy of the root object with the (deeply nested) field(s) set to the new value.
     */
    def setTo(v: U): T = doModify(obj, _ => v)

    /**
     * Set the value of the field(s) to a new value, if it is defined. Otherwise, returns the original object unchanged.
     *
     * @return
     *   A copy of the root object with the (deeply nested) field(s) set to the new value, if it is defined.
     */
    def setToIfDefined(v: Option[U]): T = v.fold(obj)(setTo)

    /**
     * Set the value of the field(s) to a new value, if the condition is true. Otherwise, returns the original object unchanged.
     *
     * @return
     *   A copy of the root object with the (deeply nested) field(s) set to the new value, if `condition` is true.
     */
    def setToIf(condition: Boolean)(v: => U): T = if (condition) setTo(v) else obj
  }

  implicit class AbstractPathModifyPimp[T, U](f1: T => PathModify[T, U]) {
    def andThenModify[V](f2: U => PathModify[U, V]): T => PathModify[T, V] = { t: T =>
      PathModify[T, V](t, (t, vv) => f1(t).doModify(t, u => f2(u).doModify(u, vv)))
    }
  }

  def modifyLens[T]: LensHelper[T] = LensHelper[T]()

  def modifyAllLens[T]: MultiLensHelper[T] = MultiLensHelper[T]()

  final case class LensHelper[T] private () {

    def apply[U](path: T => U): PathLazyModify[T, U] = macro QuicklensMacros.modifyLazy_impl[T, U]
  }

  final case class MultiLensHelper[T] private () {

    def apply[U](path1: T => U, paths: (T => U)*): PathLazyModify[T, U] = macro QuicklensMacros.modifyLazyAll_impl[T, U]
  }

  final case class PathLazyModify[T, U](doModify: (T, U => U) => T) {

    self =>

    /**
     * see [[PathModify.using]]
     */
    def using(mod: U => U): T => T = obj => doModify(obj, mod)

    /**
     * see [[PathModify.usingIf]]
     */
    def usingIf(condition: Boolean)(mod: U => U): T => T =
      obj =>
        if (condition) doModify(obj, mod)
        else obj

    /**
     * see [[PathModify.setTo]]
     */
    def setTo(v: U): T => T = obj => doModify(obj, _ => v)

    /**
     * see [[PathModify.setToIfDefined]]
     */
    def setToIfDefined(v: Option[U]): T => T = v.fold((obj: T) => obj)(setTo)

    /**
     * see [[PathModify.setToIf]]
     */
    def setToIf(condition: Boolean)(v: => U): T => T =
      if (condition) setTo(v)
      else obj => obj

    /**
     * see [[AbstractPathModifyPimp]]
     */
    def andThenModify[V](f2: PathLazyModify[U, V]): PathLazyModify[T, V] =
      PathLazyModify[T, V]((t, vv) => self.doModify(t, u => f2.doModify(u, vv)))
  }

  trait QuicklensFunctor[F[_], A] {
    def map(fa: F[A])(f: A => A): F[A]
    def each(fa: F[A])(f: A => A): F[A] = map(fa)(f)
    def eachWhere(fa: F[A], p: A => Boolean)(f: A => A): F[A] = map(fa) { a =>
      if (p(a)) f(a) else a
    }
  }

  implicit def optionQuicklensFunctor[A]: QuicklensFunctor[Option, A] with QuicklensSingleAtFunctor[Option, A] =
    new QuicklensFunctor[Option, A] with QuicklensSingleAtFunctor[Option, A] {
      override def map(fa: Option[A])(f: A => A): Option[A] = fa.map(f)
      override def at(fa: Option[A])(f: A => A): Option[A] = Some(fa.map(f).get)
      override def atOrElse(fa: Option[A], default: => A)(f: A => A): Option[A] = fa.orElse(Some(default)).map(f)
      override def index(fa: Option[A])(f: A => A): Option[A] = fa.map(f)
    }

  // Currently only used for [[Option]], but could be used for [[Right]]-biased [[Either]]s.
  trait QuicklensSingleAtFunctor[F[_], T] {
    def at(fa: F[T])(f: T => T): F[T]
    def atOrElse(fa: F[T], default: => T)(f: T => T): F[T]
    def index(fa: F[T])(f: T => T): F[T]
  }

  implicit class QuicklensSingleAt[F[_], T](t: F[T])(implicit f: QuicklensSingleAtFunctor[F, T]) {
    @compileTimeOnly(canOnlyBeUsedInsideModify("at"))
    def at: T = sys.error("")

    @compileTimeOnly(canOnlyBeUsedInsideModify("at"))
    def atOrElse(default: => T): T = sys.error("")

    @compileTimeOnly(canOnlyBeUsedInsideModify("index"))
    def index: T = sys.error("")
  }

  implicit def traversableQuicklensFunctor[F[_], A](implicit
      fac: Factory[A, F[A]],
      ev: F[A] => Iterable[A]
  ): QuicklensFunctor[F, A] =
    new QuicklensFunctor[F, A] {
      override def map(fa: F[A])(f: A => A) = ev(fa).map(f).to(fac)
    }

  implicit class QuicklensAt[F[_], T](t: F[T])(implicit f: QuicklensAtFunctor[F, T]) {
    @compileTimeOnly(canOnlyBeUsedInsideModify("at"))
    def at(idx: Int): T = sys.error("")
    @compileTimeOnly(canOnlyBeUsedInsideModify("index"))
    def index(idx: Int): T = sys.error("")
  }

  trait QuicklensAtFunctor[F[_], T] {
    def at(fa: F[T], idx: Int)(f: T => T): F[T]
    def index(fa: F[T], idx: Int)(f: T => T): F[T]
  }

  implicit class QuicklensMapAt[M[KT, TT], K, T](t: M[K, T])(implicit
      f: QuicklensMapAtFunctor[M, K, T]
  ) {
    @compileTimeOnly(canOnlyBeUsedInsideModify("at"))
    def at(idx: K): T = sys.error("")

    @compileTimeOnly(canOnlyBeUsedInsideModify("atOrElse"))
    def atOrElse(idx: K, default: => T): T = sys.error("")

    @compileTimeOnly(canOnlyBeUsedInsideModify("index"))
    def index(idx: K): T = sys.error("")

    @compileTimeOnly(canOnlyBeUsedInsideModify("each"))
    def each: T = sys.error("")
  }

  trait QuicklensMapAtFunctor[F[_, _], K, T] {
    def at(fa: F[K, T], idx: K)(f: T => T): F[K, T]
    def atOrElse(fa: F[K, T], idx: K, default: => T)(f: T => T): F[K, T]
    def index(fa: F[K, T], idx: K)(f: T => T): F[K, T]
    def each(fa: F[K, T])(f: T => T): F[K, T]
  }

  implicit def mapQuicklensFunctor[M[KT, TT] <: Map[KT, TT], K, T](implicit
      fac: Factory[(K, T), M[K, T]]
  ): QuicklensMapAtFunctor[M, K, T] = new QuicklensMapAtFunctor[M, K, T] {
    override def at(fa: M[K, T], key: K)(f: T => T): M[K, T] =
      fa.updated(key, f(fa(key))).to(fac)
    override def atOrElse(fa: M[K, T], key: K, default: => T)(f: T => T): M[K, T] =
      fa.updated(key, f(fa.getOrElse(key, default))).to(fac)
    override def index(fa: M[K, T], key: K)(f: T => T): M[K, T] =
      fa.get(key).map(f).fold(fa)(t => fa.updated(key, t).to(fac))
    override def each(fa: M[K, T])(f: (T) => T): M[K, T] =
      fa.view.mapValues(f).to(fac)
  }

  @SuppressWarnings(Array("org.wartremover.warts.SeqApply", "org.wartremover.warts.SeqUpdated"))
  implicit def seqQuicklensAtFunctor[F[_], T](implicit
      fac: Factory[T, F[T]],
      ev: F[T] => Seq[T]
  ): QuicklensAtFunctor[F, T] =
    new QuicklensAtFunctor[F, T] {
      override def at(fa: F[T], idx: Int)(f: T => T): F[T] =
        fa.updated(idx, f(fa(idx))).to(fac)
      override def index(fa: F[T], idx: Int)(f: T => T): F[T] =
        if (idx < fa.size) fa.updated(idx, f(fa(idx))).to(fac) else fa
    }

  implicit class QuicklensWhen[A](value: A) {
    @compileTimeOnly(canOnlyBeUsedInsideModify("when"))
    def when[B <: A]: B = sys.error("")
  }

  implicit class QuicklensEither[T[_, _], L, R](e: T[L, R])(implicit f: QuicklensEitherFunctor[T, L, R]) {
    @compileTimeOnly(canOnlyBeUsedInsideModify("eachLeft"))
    def eachLeft: L = sys.error("")

    @compileTimeOnly(canOnlyBeUsedInsideModify("eachRight"))
    def eachRight: R = sys.error("")
  }

  trait QuicklensEitherFunctor[T[_, _], L, R] {
    def eachLeft(e: T[L, R])(f: L => L): T[L, R]
    def eachRight(e: T[L, R])(f: R => R): T[L, R]
  }

  implicit def eitherQuicklensFunctor[T[_, _], L, R]: QuicklensEitherFunctor[Either, L, R] =
    new QuicklensEitherFunctor[Either, L, R] {
      override def eachLeft(e: Either[L, R])(f: (L) => L) = e.left.map(f)
      override def eachRight(e: Either[L, R])(f: (R) => R) = e.map(f)
    }
}
