package org.github.holgerbrandl.desimuk

import java.util.*

class  ComponentQueue <T : Component>(env: Environment=null, q: Queue<T> = LinkedList())  :  Queue<T>  by q{

}