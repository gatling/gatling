/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

package io.gatling.core.json

import java.util

/**
 * High-performance JSON string validator. Validates JSON syntax per RFC 8259 without building a DOM tree.
 */
@SuppressWarnings(Array("org.wartremover.warts.Return"))
private[gatling] object JsonValidator {

  private final val Threshold: Int = 80

  private final val ArrFirst: Byte = 0
  private final val ArrNext: Byte = 1
  private final val ArrAfter: Byte = 2
  private final val ObjFirst: Byte = 3
  private final val ObjNext: Byte = 4
  private final val ObjAfterKey: Byte = 5
  private final val ObjExpectVal: Byte = 6
  private final val ObjAfterVal: Byte = 7

  private val SharedHexTable: Array[Byte] = {
    val table = new Array[Byte](128)
    util.Arrays.fill(table, -1.toByte)
    var c = '0'.toInt
    while (c <= '9') { table(c) = (c - '0').toByte; c += 1 }
    c = 'a'
    while (c <= 'f') { table(c) = (c - 'a' + 10).toByte; c += 1 }
    c = 'A'
    while (c <= 'F') { table(c) = (c - 'A' + 10).toByte; c += 1 }
    table
  }

  def isValid(s: String): Boolean = {
    if (s == null || s.isEmpty) return false
    if (s.length < Threshold) PathShort.isValid(s)
    else PathLong.isValid(s)
  }

  private object PathShort {

    private final val InitialStackCap: Int = 32

    private final class Ctx(val s: String) {
      val n: Int = s.length
      var p: Int = 0
      var stack: Array[Byte] = new Array[Byte](InitialStackCap)
      var depth: Int = 0
    }

    def isValid(s: String): Boolean = {
      val ctx = new Ctx(s)
      skipWs(ctx)
      if (ctx.p >= ctx.n) return false

      val c0 = ch(ctx)
      if (c0 == '{') {
        ctx.p += 1; pushState(ctx, ObjFirst)
      } else if (c0 == '[') {
        ctx.p += 1; pushState(ctx, ArrFirst)
      } else {
        if (!parsePrimitive(ctx)) return false
        skipWs(ctx); return ctx.p == ctx.n
      }

      var valid = true
      while (valid && ctx.depth > 0) {
        skipWs(ctx)
        if (ctx.p >= ctx.n) { valid = false }
        else {
          val top = ctx.depth - 1
          val st = ctx.stack(top)
          if (st <= ArrAfter) valid = handleArray(ctx, top, st)
          else valid = handleObject(ctx, top, st)
        }
      }
      if (!valid) return false
      skipWs(ctx); ctx.p == ctx.n
    }

    private def handleArray(ctx: Ctx, top: Int, st: Byte): Boolean = {
      if (st == ArrFirst) {
        if (ch(ctx) == ']') { ctx.p += 1; ctx.depth -= 1; return true }
        return parseValue(ctx, top, ArrAfter)
      }
      if (st == ArrNext) return parseValue(ctx, top, ArrAfter)
      val c = ch(ctx)
      if (c == ']') { ctx.p += 1; ctx.depth -= 1; return true }
      if (c == ',') { ctx.p += 1; ctx.stack(top) = ArrNext; return true }
      false
    }

    private def handleObject(ctx: Ctx, top: Int, st: Byte): Boolean = {
      if (st == ObjFirst) {
        if (ch(ctx) == '}') { ctx.p += 1; ctx.depth -= 1; return true }
        if (ch(ctx) != '"' || !parseString(ctx)) return false
        ctx.stack(top) = ObjAfterKey; return true
      }
      if (st == ObjNext) {
        if (ch(ctx) != '"' || !parseString(ctx)) return false
        ctx.stack(top) = ObjAfterKey; return true
      }
      if (st == ObjAfterKey) {
        if (ch(ctx) != ':') return false
        ctx.p += 1; ctx.stack(top) = ObjExpectVal; return true
      }
      if (st == ObjExpectVal) return parseValue(ctx, top, ObjAfterVal)
      val c = ch(ctx)
      if (c == '}') { ctx.p += 1; ctx.depth -= 1; return true }
      if (c == ',') { ctx.p += 1; ctx.stack(top) = ObjNext; return true }
      false
    }

    private def parseValue(ctx: Ctx, top: Int, afterState: Byte): Boolean = {
      val c = ch(ctx)
      if (c == '{') { ctx.stack(top) = afterState; ctx.p += 1; pushState(ctx, ObjFirst); return true }
      if (c == '[') { ctx.stack(top) = afterState; ctx.p += 1; pushState(ctx, ArrFirst); return true }
      if (parsePrimitive(ctx)) { ctx.stack(top) = afterState; return true }
      false
    }

    private def skipWs(ctx: Ctx): Unit =
      while (ctx.p < ctx.n) {
        (ch(ctx): @scala.annotation.switch) match {
          case ' ' | '\n' | '\r' | '\t' => ctx.p += 1
          case _                        => return
        }
      }

    private def pushState(ctx: Ctx, state: Byte): Unit = {
      if (ctx.depth == ctx.stack.length) {
        ctx.stack = util.Arrays.copyOf(ctx.stack, ctx.depth + InitialStackCap)
      }
      ctx.stack(ctx.depth) = state; ctx.depth += 1
    }

    private def parsePrimitive(ctx: Ctx): Boolean = {
      val c = ch(ctx)
      if (c == '"') return parseString(ctx)
      if (c == 't') return parseLiteral(ctx, 'r', 'u', 'e', 4)
      if (c == 'f') return parseLiteral(ctx, 'a', 'l', 's', 5)
      if (c == 'n') return parseLiteral(ctx, 'u', 'l', 'l', 4)
      if (c == '-' || (c >= '0' && c <= '9')) return parseNumber(ctx)
      false
    }

    private def parseLiteral(ctx: Ctx, c1: Char, c2: Char, c3: Char, len: Int): Boolean = {
      if (ctx.p + len > ctx.n) return false
      if (ch(ctx, 1) != c1 || ch(ctx, 2) != c2 || ch(ctx, 3) != c3) return false
      if (len == 5 && ch(ctx, 4) != 'e') return false
      ctx.p += len; true
    }

    private def parseString(ctx: Ctx): Boolean = {
      ctx.p += 1
      while (ctx.p < ctx.n) {
        var brk = false
        while (!brk && ctx.p < ctx.n) {
          val c = ch(ctx)
          if (c == '"' || c == '\\' || c < 0x20) brk = true
          else ctx.p += 1
        }
        if (ctx.p >= ctx.n) return false

        val c = ch(ctx)
        if (c == '"') { ctx.p += 1; return true }
        if (c == '\\') {
          ctx.p += 1
          if (ctx.p >= ctx.n) return false
          val e = ch(ctx)
          if (
            e == '"' || e == '\\' || e == '/' || e == 'b' ||
            e == 'f' || e == 'n' || e == 'r' || e == 't'
          ) {
            ctx.p += 1
          } else if (e == 'u') {
            ctx.p += 1
            val cp = parseUnicodeEscape(ctx)
            if (cp < 0) return false
            if (cp >= 0xd800 && cp <= 0xdbff) {
              if (ctx.p + 1 >= ctx.n || ch(ctx) != '\\' || ch(ctx, 1) != 'u') return false
              ctx.p += 2
              val lo = parseUnicodeEscape(ctx)
              if (lo < 0xdc00 || lo > 0xdfff) return false
            } else if (cp >= 0xdc00 && cp <= 0xdfff) return false
          } else return false
        } else {
          if (c < 0x20) return false
          ctx.p += 1
        }
      }
      false
    }

    private def parseUnicodeEscape(ctx: Ctx): Int = {
      if (ctx.p + 4 > ctx.n) return -1
      var v = 0; var i = 0
      while (i < 4) {
        val c = ch(ctx, i)
        val h =
          if (c >= '0' && c <= '9') c - '0'
          else if (c >= 'a' && c <= 'f') c - 'a' + 10
          else if (c >= 'A' && c <= 'F') c - 'A' + 10
          else return -1
        v = (v << 4) | h; i += 1
      }
      ctx.p += 4; v
    }

    private def parseNumber(ctx: Ctx): Boolean = {
      if (ch(ctx) == '-') { ctx.p += 1; if (ctx.p >= ctx.n) return false }
      val c0 = ch(ctx)
      if (c0 == '0') {
        ctx.p += 1
        if (ctx.p < ctx.n && ch(ctx) >= '0' && ch(ctx) <= '9') return false
      } else if (c0 >= '1' && c0 <= '9') {
        ctx.p += 1
        while (ctx.p < ctx.n && ch(ctx) >= '0' && ch(ctx) <= '9') ctx.p += 1
      } else return false

      if (ctx.p < ctx.n && ch(ctx) == '.') {
        ctx.p += 1
        if (ctx.p >= ctx.n || ch(ctx) < '0' || ch(ctx) > '9') return false
        ctx.p += 1
        while (ctx.p < ctx.n && ch(ctx) >= '0' && ch(ctx) <= '9') ctx.p += 1
      }
      if (ctx.p < ctx.n && (ch(ctx) == 'e' || ch(ctx) == 'E')) {
        ctx.p += 1
        if (ctx.p >= ctx.n) return false
        if (ch(ctx) == '+' || ch(ctx) == '-') { ctx.p += 1; if (ctx.p >= ctx.n) return false }
        if (ch(ctx) < '0' || ch(ctx) > '9') return false
        ctx.p += 1
        while (ctx.p < ctx.n && ch(ctx) >= '0' && ch(ctx) <= '9') ctx.p += 1
      }
      true
    }

    @inline private def ch(ctx: Ctx): Char = ctx.s.charAt(ctx.p)
    @inline private def ch(ctx: Ctx, offset: Int): Char = ctx.s.charAt(ctx.p + offset)
  }

  private object PathLong {

    private final val InitialStackCap: Int = 24
    private val HexTable: Array[Byte] = SharedHexTable

    private val TlBuf: ThreadLocal[Array[Char]] =
      ThreadLocal.withInitial[Array[Char]](() => new Array[Char](4096))

    private final class Ctx(s: String) {
      val n: Int = s.length
      val buf: Array[Char] = acquireBuffer(n)
      var p: Int = 0
      var stack: Array[Byte] = new Array[Byte](InitialStackCap)
      var depth: Int = 0
      s.getChars(0, n, buf, 0)
    }

    def isValid(s: String): Boolean = {
      val ctx = new Ctx(s)
      skipWs(ctx)
      if (ctx.p >= ctx.n) return false

      val c0 = ch(ctx)
      if (c0 == '{') {
        ctx.p += 1; pushState(ctx, ObjFirst)
      } else if (c0 == '[') {
        ctx.p += 1; pushState(ctx, ArrFirst)
      } else {
        if (!parsePrimitive(ctx)) return false
        skipWs(ctx); return ctx.p == ctx.n
      }

      var valid = true
      while (valid && ctx.depth > 0) {
        skipWs(ctx)
        if (ctx.p >= ctx.n) { valid = false }
        else {
          val top = ctx.depth - 1
          val st = ctx.stack(top)
          if (st <= ArrAfter) valid = handleArray(ctx, top, st)
          else valid = handleObject(ctx, top, st)
        }
      }
      if (!valid) return false
      skipWs(ctx); ctx.p == ctx.n
    }

    private def handleArray(ctx: Ctx, top: Int, st: Byte): Boolean = {
      if (st == ArrFirst) {
        if (ch(ctx) == ']') { ctx.p += 1; ctx.depth -= 1; return true }
        return parseValue(ctx, top, ArrAfter)
      }
      if (st == ArrNext) return parseValue(ctx, top, ArrAfter)
      val c = ch(ctx)
      if (c == ']') { ctx.p += 1; ctx.depth -= 1; return true }
      if (c == ',') { ctx.p += 1; ctx.stack(top) = ArrNext; return true }
      false
    }

    private def handleObject(ctx: Ctx, top: Int, st: Byte): Boolean = {
      if (st == ObjFirst) {
        if (ch(ctx) == '}') { ctx.p += 1; ctx.depth -= 1; return true }
        if (ch(ctx) != '"' || !parseString(ctx)) return false
        ctx.stack(top) = ObjAfterKey; return true
      }
      if (st == ObjNext) {
        if (ch(ctx) != '"' || !parseString(ctx)) return false
        ctx.stack(top) = ObjAfterKey; return true
      }
      if (st == ObjAfterKey) {
        if (ch(ctx) != ':') return false
        ctx.p += 1; ctx.stack(top) = ObjExpectVal; return true
      }
      if (st == ObjExpectVal) return parseValue(ctx, top, ObjAfterVal)
      val c = ch(ctx)
      if (c == '}') { ctx.p += 1; ctx.depth -= 1; return true }
      if (c == ',') { ctx.p += 1; ctx.stack(top) = ObjNext; return true }
      false
    }

    private def parseValue(ctx: Ctx, top: Int, afterState: Byte): Boolean = {
      val c = ch(ctx)
      if (c == '{') { ctx.stack(top) = afterState; ctx.p += 1; pushState(ctx, ObjFirst); return true }
      if (c == '[') { ctx.stack(top) = afterState; ctx.p += 1; pushState(ctx, ArrFirst); return true }
      if (parsePrimitive(ctx)) { ctx.stack(top) = afterState; return true }
      false
    }

    private def skipWs(ctx: Ctx): Unit =
      while (ctx.p < ctx.n) {
        (ch(ctx): @scala.annotation.switch) match {
          case ' ' | '\n' | '\r' | '\t' => ctx.p += 1
          case _                        => return
        }
      }

    private def pushState(ctx: Ctx, state: Byte): Unit = {
      if (ctx.depth == ctx.stack.length) {
        ctx.stack = util.Arrays.copyOf(ctx.stack, ctx.depth + InitialStackCap)
      }
      ctx.stack(ctx.depth) = state; ctx.depth += 1
    }

    private def parsePrimitive(ctx: Ctx): Boolean = {
      val c = ch(ctx)
      if (c == '"') return parseString(ctx)
      if (c == 't') return parseLiteral(ctx, 'r', 'u', 'e', 4)
      if (c == 'f') return parseLiteral(ctx, 'a', 'l', 's', 5)
      if (c == 'n') return parseLiteral(ctx, 'u', 'l', 'l', 4)
      if (c == '-' || (c >= '0' && c <= '9')) return parseNumber(ctx)
      false
    }

    private def parseLiteral(ctx: Ctx, c1: Char, c2: Char, c3: Char, len: Int): Boolean = {
      if (ctx.p + len > ctx.n) return false
      if (ch(ctx, 1) != c1 || ch(ctx, 2) != c2 || ch(ctx, 3) != c3) return false
      if (len == 5 && ch(ctx, 4) != 'e') return false
      ctx.p += len; true
    }

    private def parseString(ctx: Ctx): Boolean = {
      ctx.p += 1
      while (ctx.p < ctx.n) {
        val c = ch(ctx)
        if (c == '"') { ctx.p += 1; return true }
        if (c == '\\') {
          ctx.p += 1
          if (ctx.p >= ctx.n) return false
          val e = ch(ctx)
          if (
            e == '"' || e == '\\' || e == '/' || e == 'b' ||
            e == 'f' || e == 'n' || e == 'r' || e == 't'
          ) {
            ctx.p += 1
          } else if (e == 'u') {
            ctx.p += 1
            val cp = parseUnicodeEscape(ctx)
            if (cp < 0) return false
            if (cp >= 0xd800 && cp <= 0xdbff) {
              if (ctx.p + 1 >= ctx.n || ch(ctx) != '\\' || ch(ctx, 1) != 'u') return false
              ctx.p += 2
              val lo = parseUnicodeEscape(ctx)
              if (lo < 0xdc00 || lo > 0xdfff) return false
            } else if (cp >= 0xdc00 && cp <= 0xdfff) return false
          } else return false
        } else {
          if (c < 0x20) return false
          ctx.p += 1
        }
      }
      false
    }

    private def parseUnicodeEscape(ctx: Ctx): Int = {
      if (ctx.p + 4 > ctx.n) return -1
      val ht = HexTable
      var v = 0; var i = 0
      while (i < 4) {
        val c = ch(ctx, i)
        if (c >= ht.length) return -1
        val h = ht(c)
        if (h < 0) return -1
        v = (v << 4) | h; i += 1
      }
      ctx.p += 4; v
    }

    private def parseNumber(ctx: Ctx): Boolean = {
      if (ch(ctx) == '-') { ctx.p += 1; if (ctx.p >= ctx.n) return false }
      val c0 = ch(ctx)
      if (c0 == '0') {
        ctx.p += 1
        if (ctx.p < ctx.n && ch(ctx) >= '0' && ch(ctx) <= '9') return false
      } else if (c0 >= '1' && c0 <= '9') {
        ctx.p += 1
        while (ctx.p < ctx.n && ch(ctx) >= '0' && ch(ctx) <= '9') ctx.p += 1
      } else return false

      if (ctx.p < ctx.n && ch(ctx) == '.') {
        ctx.p += 1
        if (ctx.p >= ctx.n || ch(ctx) < '0' || ch(ctx) > '9') return false
        ctx.p += 1
        while (ctx.p < ctx.n && ch(ctx) >= '0' && ch(ctx) <= '9') ctx.p += 1
      }
      if (ctx.p < ctx.n && (ch(ctx) == 'e' || ch(ctx) == 'E')) {
        ctx.p += 1
        if (ctx.p >= ctx.n) return false
        if (ch(ctx) == '+' || ch(ctx) == '-') { ctx.p += 1; if (ctx.p >= ctx.n) return false }
        if (ch(ctx) < '0' || ch(ctx) > '9') return false
        ctx.p += 1
        while (ctx.p < ctx.n && ch(ctx) >= '0' && ch(ctx) <= '9') ctx.p += 1
      }
      true
    }

    private def acquireBuffer(required: Int): Array[Char] = {
      var buf = TlBuf.get()
      if (buf.length >= required) return buf
      var cap = buf.length
      while (cap < required) cap <<= 1
      buf = new Array[Char](cap)
      TlBuf.set(buf)
      buf
    }

    @inline private def ch(ctx: Ctx): Char = ctx.buf(ctx.p)
    @inline private def ch(ctx: Ctx, offset: Int): Char = ctx.buf(ctx.p + offset)
  }
}
