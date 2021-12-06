package paintbox.ui

import paintbox.binding.Var


sealed class Anchor {

    /**
     * The offsets are relative to the top left corner of the anchor parent.
     * Positive X offset is further RIGHT, positive Y offset is further DOWN.
     */
    object TopLeft : Anchor() {
        override fun configure(element: UIElement, offsetX: Float, offsetY: Float) {
            element.bounds.x.set(offsetX)
            element.bounds.y.set(offsetY)
        }
        
        override fun configure(element: UIElement, offsetX: Var.Context.() -> Float,
                               offsetY: Var.Context.() -> Float) {
            element.bounds.x.bind {
                offsetX.invoke(this)
            }
            element.bounds.y.bind {
                offsetY.invoke(this)
            }
        }
    }

    /**
     * The offsets are relative to the midpoint of the left edge of the anchor parent.
     * Positive X offset is further RIGHT, positive Y offset is further DOWN.
     */
    object CentreLeft : Anchor() {
        override fun configure(element: UIElement, offsetX: Float, offsetY: Float) {
            val parent = element.parent
            element.bounds.x.set(offsetX)
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use() ?: 0f) / 2f - (element.bounds.height.use() / 2f) + offsetY
            }
        }

        override fun configure(element: UIElement, offsetX: Var.Context.() -> Float,
                               offsetY: Var.Context.() -> Float) {
            val parent = element.parent
            element.bounds.x.bind {
                offsetX.invoke(this)
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use() ?: 0f) / 2f - (element.bounds.height.use() / 2f) + offsetY.invoke(this)
            }
        }
    }

    /**
     * The offsets are relative to the bottom left corner of the anchor parent.
     * Positive X offset is further RIGHT, positive Y offset is further DOWN.
     */
    object BottomLeft : Anchor() {
        override fun configure(element: UIElement, offsetX: Float, offsetY: Float) {
            val parent = element.parent
            element.bounds.x.set(offsetX)
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use() ?: 0f) - (element.bounds.height.use()) + offsetY
            }
        }

        override fun configure(element: UIElement, offsetX: Var.Context.() -> Float,
                               offsetY: Var.Context.() -> Float) {
            val parent = element.parent
            element.bounds.x.bind {
                offsetX.invoke(this)
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use()
                        ?: 0f) - (element.bounds.height.use()) + offsetY.invoke(this)
            }
        }
    }

    /**
     * The offsets are relative to the top right corner of the anchor parent.
     * Positive X offset is further RIGHT, positive Y offset is further DOWN.
     */
    object TopRight : Anchor() {
        override fun configure(element: UIElement, offsetX: Float, offsetY: Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) - (element.bounds.width.use()) + offsetX
            }
            element.bounds.y.set(offsetY)
        }

        override fun configure(element: UIElement, offsetX: Var.Context.() -> Float,
                               offsetY: Var.Context.() -> Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) - (element.bounds.width.use()) + offsetX.invoke(this)
            }
            element.bounds.y.bind {
                offsetY.invoke(this)
            }
        }
    }

    /**
     * The offsets are relative to the midpoint of the right edge of the anchor parent.
     * Positive X offset is further RIGHT, positive Y offset is further DOWN.
     */
    object CentreRight : Anchor() {
        override fun configure(element: UIElement, offsetX: Float, offsetY: Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) - (element.bounds.width.use()) + offsetX
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use() ?: 0f) / 2f - (element.bounds.height.use() / 2f) + offsetY
            }
        }

        override fun configure(element: UIElement, offsetX: Var.Context.() -> Float,
                               offsetY: Var.Context.() -> Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) - (element.bounds.width.use()) + offsetX.invoke(this)
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use() ?: 0f) / 2f - (element.bounds.height.use() / 2f) + offsetY.invoke(this)
            }
        }
    }

    /**
     * The offsets are relative to the bottom right corner of the anchor parent.
     * Positive X offset is further RIGHT, positive Y offset is further DOWN.
     */
    object BottomRight : Anchor() {
        override fun configure(element: UIElement, offsetX: Float, offsetY: Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) - (element.bounds.width.use()) + offsetX
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use() ?: 0f) - (element.bounds.height.use()) + offsetY
            }
        }

        override fun configure(element: UIElement, offsetX: Var.Context.() -> Float,
                               offsetY: Var.Context.() -> Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) - (element.bounds.width.use()) + offsetX.invoke(this)
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use()
                        ?: 0f) - (element.bounds.height.use()) + offsetY.invoke(this)
            }
        }
    }

    /**
     * The offsets are relative to the midpoint of the top edge of the anchor parent.
     * Positive X offset is further RIGHT, positive Y offset is further DOWN.
     */
    object TopCentre : Anchor() {
        override fun configure(element: UIElement, offsetX: Float, offsetY: Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) / 2f - (element.bounds.width.use() / 2f) + offsetX
            }
            element.bounds.y.set(offsetY)
        }

        override fun configure(element: UIElement, offsetX: Var.Context.() -> Float,
                               offsetY: Var.Context.() -> Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) / 2f - (element.bounds.width.use() / 2f) + offsetX.invoke(this)
            }
            element.bounds.y.bind {
                offsetY.invoke(this)
            }
        }
    }

    /**
     * The offsets are relative to the midpoint of the bottom edge of the anchor parent.
     * Positive X offset is further RIGHT, positive Y offset is further DOWN.
     */
    object BottomCentre : Anchor() {
        override fun configure(element: UIElement, offsetX: Float, offsetY: Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) / 2f - (element.bounds.width.use() / 2f) + offsetX
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use() ?: 0f) - (element.bounds.height.use()) + offsetY
            }
        }
        
        override fun configure(element: UIElement, offsetX: Var.Context.() -> Float,
                               offsetY: Var.Context.() -> Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) / 2f - (element.bounds.width.use() / 2f) + offsetX.invoke(this)
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use()
                        ?: 0f) - (element.bounds.height.use()) + offsetY.invoke(this)
            }
        }
    }

    /**
     * The offsets are relative to the centre-point of the anchor parent.
     * Positive X offset is further RIGHT, positive Y offset is further DOWN.
     */
    object Centre : Anchor() {
        override fun configure(element: UIElement, offsetX: Float, offsetY: Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) / 2f - (element.bounds.width.use() / 2f) + offsetX
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use() ?: 0f) / 2f - (element.bounds.height.use() / 2f) + offsetY
            }
        }

        override fun configure(element: UIElement, offsetX: Var.Context.() -> Float,
                               offsetY: Var.Context.() -> Float) {
            val parent = element.parent
            element.bounds.x.bind {
                (parent.use()?.contentZone?.width?.use() ?: 0f) / 2f - (element.bounds.width.use() / 2f) + offsetX.invoke(this)
            }
            element.bounds.y.bind {
                (parent.use()?.contentZone?.height?.use() ?: 0f) / 2f - (element.bounds.height.use() / 2f) + offsetY.invoke(this)
            }
        }
    }

    /**
     * Configures the [element] based on this [Anchor] with constant offsets. 
     */
    abstract fun configure(element: UIElement, offsetX: Float = 0f, offsetY: Float = 0f)
    
    /**
     * Configures the [element] based on this [Anchor] with bindable offsets to the bounds x/y [Var] context.
     * 
     * The context in the [offsetX]/[offsetY] function parameters is the [Var.Context] of the bounds x/y [Var].
     */
    abstract fun configure(element: UIElement,
                           offsetX: Var.Context.() -> Float = { 0f },
                           offsetY: Var.Context.() -> Float = { 0f })
    
    /**
     * Configures the [element] based on this [Anchor] with constant offsets of 0.
     */
    final fun configure(element: UIElement) = configure(element, 0f, 0f)

}