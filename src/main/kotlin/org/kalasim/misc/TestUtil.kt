package org.kalasim.misc

import org.kalasim.Component

class TestUtil {

    companion object {
        fun requests(component: Component) = component.requests
        fun claims(component: Component) = component.claims
    }

}