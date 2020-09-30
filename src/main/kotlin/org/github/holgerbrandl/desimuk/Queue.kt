package org.github.holgerbrandl.desimuk

import java.util.*

class  ComponentQueue <T : Component>(q: Queue<T> = LinkedList())  :  Queue<T>  by q{

    // add queue statistics etc.

}