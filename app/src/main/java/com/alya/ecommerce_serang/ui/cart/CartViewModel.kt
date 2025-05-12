package com.alya.ecommerce_serang.ui.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alya.ecommerce_serang.data.api.dto.UpdateCart
import com.alya.ecommerce_serang.data.api.response.customer.cart.CartItemsItem
import com.alya.ecommerce_serang.data.api.response.customer.cart.DataItemCart
import com.alya.ecommerce_serang.data.repository.OrderRepository
import com.alya.ecommerce_serang.data.repository.Result
import kotlinx.coroutines.launch

class CartViewModel(private val repository: OrderRepository) : ViewModel() {

    private val _cartItems = MutableLiveData<List<DataItemCart>>()
    val cartItems: LiveData<List<DataItemCart>> = _cartItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _totalPrice = MutableLiveData<Int>(0)
    val totalPrice: LiveData<Int> = _totalPrice

    private val _selectedItems = MutableLiveData<HashSet<Int>>(HashSet())
    val selectedItems: LiveData<HashSet<Int>> = _selectedItems

    private val _selectedStores = MutableLiveData<HashSet<Int>>(HashSet())
    val selectedStores: LiveData<HashSet<Int>> = _selectedStores

    private val _totalSelectedCount = MutableLiveData<Int>(0)
    val totalSelectedCount: LiveData<Int> = _totalSelectedCount

    // Track the currently active store ID for checkout
    private val _activeStoreId = MutableLiveData<Int?>(null)
    val activeStoreId: LiveData<Int?> = _activeStoreId

    // Track if all items are selected
    private val _allSelected = MutableLiveData<Boolean>(false)
    val allSelected: LiveData<Boolean> = _allSelected

    fun getCart() {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            when (val result = repository.getCart()) {
                is com.alya.ecommerce_serang.data.repository.Result.Success -> {
                    _cartItems.value = result.data
                    _isLoading.value = false
                }
                is com.alya.ecommerce_serang.data.repository.Result.Error -> {
                    _errorMessage.value = result.exception.message
                    _isLoading.value = false
                }
                is Result.Loading -> {
                    null
                }
            }
        }
    }

    fun updateCartItem(cartItemId: Int, quantity: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val updateCart = UpdateCart(cartItemId, quantity)
                val result = repository.updateCart(updateCart)

                if (result is com.alya.ecommerce_serang.data.repository.Result.Success) {
                    // Refresh cart data after successful update
                    getCart()
                    calculateTotalPrice()
                } else {
                    _errorMessage.value = (result as com.alya.ecommerce_serang.data.repository.Result.Error).exception.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCartItem(cartItemId: Int) {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = repository.deleteCartItem(cartItemId)

                if (result is com.alya.ecommerce_serang.data.repository.Result.Success) {
                    // Remove the item from selected items if it was selected
                    val currentSelectedItems = _selectedItems.value ?: HashSet()
                    if (currentSelectedItems.contains(cartItemId)) {
                        currentSelectedItems.remove(cartItemId)
                        _selectedItems.value = currentSelectedItems
                    }

                    // Refresh cart data after successful deletion
                    getCart()
                    calculateTotalPrice()
                } else {
                    _errorMessage.value = (result as Result.Error).exception.message
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleItemSelection(cartItemId: Int, storeId: Int) {
        val currentSelectedItems = _selectedItems.value ?: HashSet()
        val currentSelectedStores = _selectedStores.value ?: HashSet()

        if (currentSelectedItems.contains(cartItemId)) {
            currentSelectedItems.remove(cartItemId)

            // Check if there are no more selected items for this store
            val storeHasSelectedItems = _cartItems.value?.find { it.storeId == storeId }
                ?.cartItems?.any { currentSelectedItems.contains(it.cartItemId) } ?: false

            if (!storeHasSelectedItems) {
                currentSelectedStores.remove(storeId)

                // If this was the active store, set active store to null
                if (_activeStoreId.value == storeId) {
                    _activeStoreId.value = null
                }
            }
        } else {
            // If there's an active store different from this item's store, deselect all items first
            if (_activeStoreId.value != null && _activeStoreId.value != storeId) {
                currentSelectedItems.clear()
                currentSelectedStores.clear()
            }

            currentSelectedItems.add(cartItemId)
            currentSelectedStores.add(storeId)

            // Set the active store
            _activeStoreId.value = storeId
        }

        _selectedItems.value = currentSelectedItems
        _selectedStores.value = currentSelectedStores

        calculateTotalPrice()
        updateTotalSelectedCount()
        checkAllSelected()
    }

    fun toggleStoreSelection(storeId: Int) {
        val currentSelectedItems = _selectedItems.value ?: HashSet()
        val currentSelectedStores = _selectedStores.value ?: HashSet()
        val storeItems = _cartItems.value?.find { it.storeId == storeId }?.cartItems ?: emptyList()

        if (currentSelectedStores.contains(storeId)) {
            // Deselect all items of this store
            currentSelectedStores.remove(storeId)
            storeItems.forEach { currentSelectedItems.remove(it.cartItemId) }

            // If this was the active store, set active store to null
            if (_activeStoreId.value == storeId) {
                _activeStoreId.value = null
            }
        } else {
            // If there's another active store, deselect all items first
            if (_activeStoreId.value != null && _activeStoreId.value != storeId) {
                currentSelectedItems.clear()
                currentSelectedStores.clear()
            }

            // Select all items of this store
            currentSelectedStores.add(storeId)
            storeItems.forEach { currentSelectedItems.add(it.cartItemId) }

            // Set this as the active store
            _activeStoreId.value = storeId
        }

        _selectedItems.value = currentSelectedItems
        _selectedStores.value = currentSelectedStores

        calculateTotalPrice()
        updateTotalSelectedCount()
        checkAllSelected()
    }

    fun toggleSelectAll() {
        val allItems = _cartItems.value ?: emptyList()
        val currentSelected = _allSelected.value ?: false

        if (currentSelected) {
            // Deselect all
            _selectedItems.value = HashSet()
            _selectedStores.value = HashSet()
            _activeStoreId.value = null
            _allSelected.value = false
        } else {
            // If we have multiple stores, we need a special handling
            if (allItems.size > 1) {
                // Select all items from the first store only
                val firstStore = allItems.firstOrNull()
                if (firstStore != null) {
                    val selectedItems = HashSet<Int>()
                    firstStore.cartItems.forEach { selectedItems.add(it.cartItemId) }

                    _selectedItems.value = selectedItems
                    _selectedStores.value = HashSet<Int>().apply { add(firstStore.storeId) }
                    _activeStoreId.value = firstStore.storeId
                }
            } else {
                // Single store, select all items
                val selectedItems = HashSet<Int>()
                val selectedStores = HashSet<Int>()

                allItems.forEach { dataItem ->
                    selectedStores.add(dataItem.storeId)
                    dataItem.cartItems.forEach { cartItem ->
                        selectedItems.add(cartItem.cartItemId)
                    }
                }

                _selectedItems.value = selectedItems
                _selectedStores.value = selectedStores

                if (allItems.isNotEmpty()) {
                    _activeStoreId.value = allItems[0].storeId
                }
            }

            _allSelected.value = true
        }

        calculateTotalPrice()
        updateTotalSelectedCount()
    }

    private fun calculateTotalPrice() {
        val selectedItems = _selectedItems.value ?: HashSet()
        var total = 0

        _cartItems.value?.forEach { dataItem ->
            dataItem.cartItems.forEach { cartItem ->
                if (selectedItems.contains(cartItem.cartItemId)) {
                    total += cartItem.price * cartItem.quantity
                }
            }
        }

        _totalPrice.value = total
    }

    private fun updateTotalSelectedCount() {
        _totalSelectedCount.value = _selectedItems.value?.size ?: 0
    }

    private fun checkAllSelected() {
        val allItems = _cartItems.value ?: emptyList()
        val selectedItems = _selectedItems.value ?: HashSet()

        // If there are multiple stores, "all selected" is true only if all items of the active store are selected
        val activeStoreId = _activeStoreId.value
        val isAllSelected = if (activeStoreId != null) {
            val activeStoreItems = allItems.find { it.storeId == activeStoreId }?.cartItems ?: emptyList()
            activeStoreItems.all { selectedItems.contains(it.cartItemId) }
        } else {
            // No active store, so check if all items of any store are selected
            allItems.any { dataItem ->
                dataItem.cartItems.all { selectedItems.contains(it.cartItemId) }
            }
        }

        _allSelected.value = isAllSelected
    }

    fun prepareCheckout(): List<CartItemsItem> {
        val selectedItemsIds = _selectedItems.value ?: HashSet()
        val result = mutableListOf<CartItemsItem>()

        if (activeStoreId != null){
            _cartItems.value?.forEach { dataItem ->
                dataItem.cartItems.forEach { cartItem ->
                    if (selectedItemsIds.contains(cartItem.cartItemId)) {
                        result.add(cartItem)
                    }
                }
            }
        }

        return result
    }
}