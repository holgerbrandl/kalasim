package org.github.holgerbrandl.kalasim;

open class Resource(
    name: String? = null,
    var capacity: Double = 1.0,
    val isPreemptive: Boolean = false,
    val anonymous: Boolean = false
) : Component(name = name, process = null) {


    // should we this make readonly from outside?
    val requesters = ComponentQueue<Component>()
    val claimers = ComponentQueue<Component>()

    var claimedQuantity = 0.0
        set(x) {
            field = x
            claimedQuantityMonitor.addValue(x)
        }

    val claimedQuantityMonitor = NumericLevelMonitor()

    var availableQuantity = 0
        set(x) {
            field = x
            availableQuantityMonitor.addValue(x)
        }
    val availableQuantityMonitor = NumericLevelMonitor()


    val occupancy = NumericLevelMonitor()
    val occupancyMonitor = NumericLevelMonitor()

    val capacityMonitor = NumericLevelMonitor("Capacity of ${name}").apply {
        addValue(capacity)
    }

    fun availableQuantity(): Double = capacity - claimedQuantity

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

        }else{
            require(quantity!=null) { "quantity missing for non-anonymous resource" }

            while(requesters.isNotEmpty()){
                requesters.poll().release(this)
            }
        }
    }
}

