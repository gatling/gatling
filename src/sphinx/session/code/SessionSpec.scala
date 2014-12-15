import io.gatling.core.session.{SessionAttribute, Session}

class SessionSpec {

  {
    //#sessions-are-immutable
    val session: Session = ???

    // wrong usage
    session.set("foo", "FOO") // wrong: the result of this set call is just discarded
    session.set("bar", "BAR")

    // proper usage
    session.set("foo", "FOO").set("bar", "BAR")
    //#sessions-are-immutable

  }
  {
    //#session
    val session: Session = ???
    //#session

    //#session-attribute
    val attribute: SessionAttribute = session("foo")
    //#session-attribute
  }
}
