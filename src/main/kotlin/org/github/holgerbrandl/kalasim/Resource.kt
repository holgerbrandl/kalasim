package org.github.holgerbrandl.kalasim;

open class Resource(
    name: String? = null,
    var capacity: Double = 1.0,
    val isPreemptive: Boolean = false,
    val anonymous: Boolean = false
) : Component(name = name, process = null) {


    var minq: Double = Double.MAX_VALUE

    // should we this make readonly from outside?
    val requesters = ComponentQueue<Component>()
    val claimers = ComponentQueue<Component>()

    var claimedQuantity = 0.0
        set(x) {
            field = x

            claimedQuantityMonitor.addValue(x)
            availableQuantityMonitor.addValue(capacity - claimedQuantity)
            occupancyMonitor.addValue(if (capacity < 0) 0 else claimedQuantity)

            env.printTrace("claim ${claimedQuantity} from $this")
        }

    val claimedQuantityMonitor = NumericLevelMonitor()

    var availableQuantity = 0
        set(x) {
            field = x
            availableQuantityMonitor.addValue(x)
        }
    val availableQuantityMonitor = NumericLevelMonitor()


    val occupancyMonitor = NumericLevelMonitor()

    val capacityMonitor = NumericLevelMonitor("Capacity of ${name}").apply {
        addValue(capacity)
    }

    fun availableQuantity(): Double = capacity - claimedQuantity


    override fun tryRequest(): Boolean {
        val iterator = requesters.q.iterator()

        if (anonymous) {
            // TODO trying not implemented

            iterator.forEach {
                it.c.tryRequest()
            }
        } else {
            while (iterator.hasNext()) {
                //try honor as many requests as possible
                if (minq > (capacity - claimedQuantity + 1E-8)) {
                    break
                }
                iterator.next().c.tryRequest()
            }
        }

        return true
    }

    /** releases all claims or a specified quantity
     *
     * @param  quantity  quantity to be released. If not specified, the resource will be emptied completely.
     * For non-anonymous resources, all components claiming from this resource will be released.
     */
    fun release(quantity: Double? = null) {
        // TODO Split resource types into QuantityResource and Resource or similar
        if (anonymous) {
            val q = quantity ?: claimedQuantity

            claimedQuantity = -q
            if (claimedQuantity < 1E-8) claimedQuantity = 0.0


            occupancyMonitor.addValue(if (capacity <= 0) 0 else claimedQuantity / capacity)
            availableQuantityMonitor.addValue(capacity - claimedQuantity)

        } else {
            require(quantity != null) { "quantity missing for non-anonymous resource" }

            while (requesters.isNotEmpty()) {
                requesters.poll().release(this)
            }
        }
    }

    fun removeRequester(component: Component) {
        requesters.remove(component)
        if(requesters.isEmpty()) minq = Double.MAX_VALUE
    }
}

