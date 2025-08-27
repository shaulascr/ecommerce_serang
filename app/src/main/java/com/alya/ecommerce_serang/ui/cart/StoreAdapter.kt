package com.alya.ecommerce_serang.ui.cart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.alya.ecommerce_serang.BuildConfig.BASE_URL
import com.alya.ecommerce_serang.R
import com.alya.ecommerce_serang.data.api.response.customer.cart.CartItemsItem
import com.alya.ecommerce_serang.data.api.response.customer.cart.DataItemCart
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class StoreAdapter(
    private val onStoreCheckChanged: (Int, Boolean) -> Unit,
    private val onItemCheckChanged: (Int, Int, Boolean) -> Unit,
    private val onItemQuantityChanged: (Int, Int) -> Unit,
    private val onItemDeleted: (Int) -> Unit
) : ListAdapter<DataItemCart, RecyclerView.ViewHolder>(StoreDiffCallback()) {

    private var selectedItems = HashSet<Int>()
    private var selectedStores = HashSet<Int>()
    private var activeStoreId: Int? = null
    private var wholesaleStatusMap: Map<Int, Boolean> = mapOf()
    private var wholesalePriceMap: Map<Int, Double> = mapOf()
    private var productImages: Map<Int, String> = emptyMap()

    fun updateProductImages(newImages: Map<Int, String>) {
        productImages = newImages
        notifyDataSetChanged()
    }

    companion object {
        private const val VIEW_TYPE_STORE = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    fun updateSelectedItems(selectedItems: HashSet<Int>, selectedStores: HashSet<Int>, activeStoreId: Int?) {
        this.selectedItems = selectedItems
        this.selectedStores = selectedStores
        this.activeStoreId = activeStoreId
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        var itemCount = 0
        for (store in currentList) {
            // Store header
            if (position == itemCount) {
                return VIEW_TYPE_STORE
            }
            itemCount++

            // Check if position is in the range of this store's items
            if (position < itemCount + store.cartItems.size) {
                return VIEW_TYPE_ITEM
            }
            itemCount += store.cartItems.size
        }
        return -1
    }

    override fun getItemCount(): Int {
        var count = 0
        for (store in currentList) {
            // One for store header
            count++
            // Plus the items in this store
            count += store.cartItems.size
        }
        return count
    }

    private fun isItemSelectable(cartItemId: Int): Boolean {
        // If no items are selected, any item can be selected
        if (selectedItems.isEmpty()) return true

        // Get wholesale status of this item
        val thisItemWholesale = wholesaleStatusMap[cartItemId] ?: false

        // Get wholesale status of first selected item
        val firstSelectedId = selectedItems.first()
        val firstSelectedWholesale = wholesaleStatusMap[firstSelectedId] ?: false

        // Item can be selected if its wholesale status matches the first selected item
        return thisItemWholesale == firstSelectedWholesale
    }

    fun updateWholesaleStatus(wholesaleStatusMap: Map<Int, Boolean>, wholesalePriceMap: Map<Int, Double>) {
        this.wholesaleStatusMap = wholesaleStatusMap
        this.wholesalePriceMap = wholesalePriceMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_STORE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_store_cart, parent, false)
                StoreViewHolder(view)
            }
            VIEW_TYPE_ITEM -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_cart_product, parent, false)
                CartItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val (storeIndex, itemIndex) = getStoreAndItemIndex(position)
        val store = currentList[storeIndex]

        when (holder) {
            is StoreViewHolder -> {
                holder.bind(store, selectedStores.contains(store.storeId), activeStoreId == store.storeId) { isChecked ->
                    onStoreCheckChanged(store.storeId, isChecked)
                }
            }
            is CartItemViewHolder -> {
                val cartItem = store.cartItems[itemIndex]
                val isSelected = selectedItems.contains(cartItem.cartItemId)
                val isEnabled = activeStoreId == null || activeStoreId == store.storeId
                val isItemWholesale = wholesaleStatusMap[cartItem.cartItemId] ?: false
                val wholesalePrice = wholesalePriceMap[cartItem.cartItemId]
                val isSelectable = isItemSelectable(cartItem.cartItemId)

                holder.bind(
                    cartItem,
                    isSelected,
                    isEnabled && isSelectable,
                    isItemWholesale,
                    wholesalePrice,
                    { isChecked -> onItemCheckChanged(cartItem.cartItemId, store.storeId, isChecked) },
                    { quantity -> onItemQuantityChanged(cartItem.cartItemId, quantity) },
                    { onItemDeleted(cartItem.cartItemId) },
                    productImages
                )
            }
        }
    }

    private fun getStoreAndItemIndex(position: Int): Pair<Int, Int> {
        var itemCount = 0
        for (storeIndex in currentList.indices) {
            // Store header position
            if (position == itemCount) {
                return Pair(storeIndex, -1)
            }
            itemCount++

            // Check if position is in the range of this store's items
            val store = currentList[storeIndex]
            if (position < itemCount + store.cartItems.size) {
                return Pair(storeIndex, position - itemCount)
            }
            itemCount += store.cartItems.size
        }
        throw IllegalArgumentException("Invalid position")
    }

    class StoreViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbStore: CheckBox = itemView.findViewById(R.id.cbStore)
        private val tvStoreName: TextView = itemView.findViewById(R.id.tvStoreName)

        fun bind(store: DataItemCart, isChecked: Boolean, isActiveStore: Boolean, onCheckedChange: (Boolean) -> Unit) {
            tvStoreName.text = store.storeName

            // Set checkbox state without triggering listener
            cbStore.setOnCheckedChangeListener(null)
            cbStore.isChecked = isChecked

            // Only enable checkbox if this store is active or no store is active
            cbStore.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(isChecked)
            }
        }
    }

    class CartItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cbItem: CheckBox = itemView.findViewById(R.id.cbItem)
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProduct)
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val btnMinus: ImageButton = itemView.findViewById(R.id.btnMinus)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val btnPlus: ImageButton = itemView.findViewById(R.id.btnPlus)
        private val tvWholesaleIndicator : TextView = itemView.findViewById((R.id.tvWholesaleIndicator))
        private val quantityController: ConstraintLayout = itemView.findViewById(R.id.quantityController)

        fun bind(
            cartItem: CartItemsItem,
            isSelected: Boolean,
            isEnabled: Boolean,
            isWholesale: Boolean,
            wholesalePrice: Double?,
            onCheckedChange: (Boolean) -> Unit,
            onQuantityChanged: (Int) -> Unit,
            onDelete: () -> Unit,
            productImages: Map<Int, String>
        ) {
            // Set product name
            tvProductName.text = cartItem.productName

            // Set price based on wholesale status
            if (isWholesale && wholesalePrice != null) {
                tvPrice.text = formatCurrency(wholesalePrice.toInt())
                // Show wholesale indicator if available
                tvWholesaleIndicator?.visibility = View.VISIBLE
                tvWholesaleIndicator?.text = "Harga Grosir"
            } else {
                tvPrice.text = formatCurrency(cartItem.price)
                tvWholesaleIndicator?.visibility = View.GONE
            }

            // Set quantity
            tvQuantity.text = cartItem.quantity.toString()

            // Set checkbox state without triggering listener
            cbItem.setOnCheckedChangeListener(null)
            cbItem.isChecked = isSelected
            cbItem.isEnabled = isEnabled

            // If not enabled, visually indicate this item can't be selected
            if (!isEnabled) {
                itemView.alpha = 0.5f
            } else {
                itemView.alpha = 1.0f
            }

            // Set checkbox listener
            cbItem.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(isChecked)
            }

            val fullImageUrl = when (val img = productImages[cartItem.productId]) {
                is String -> {
                    if (img.startsWith("/")) BASE_URL + img.substring(1) else img
                }
                else -> null
            }

            Glide.with(itemView.context)
                .load(fullImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .into(ivProduct)

            // Quantity control
            btnMinus.setOnClickListener {
                val currentQty = tvQuantity.text.toString().toInt()
                if (currentQty > 1) {
                    val newQty = currentQty - 1
                    tvQuantity.text = newQty.toString()
                    onQuantityChanged(newQty)
                } else {
                    // If quantity would be 0, delete the item
                    onDelete()
                }
            }

            btnPlus.setOnClickListener {
                val currentQty = tvQuantity.text.toString().toInt()
                val newQty = currentQty + 1
                tvQuantity.text = newQty.toString()
                onQuantityChanged(newQty)
            }

            // Disable quantity controls if item is not from active store
            btnMinus.isEnabled = isEnabled
            btnPlus.isEnabled = isEnabled
        }

        private fun formatCurrency(amount: Int): String {
            val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            return format.format(amount).replace("Rp", "Rp ")
        }
    }
}

class StoreDiffCallback : DiffUtil.ItemCallback<DataItemCart>() {
    override fun areItemsTheSame(oldItem: DataItemCart, newItem: DataItemCart): Boolean {
        return oldItem.storeId == newItem.storeId
    }

    override fun areContentsTheSame(oldItem: DataItemCart, newItem: DataItemCart): Boolean {
        return oldItem == newItem
    }
}