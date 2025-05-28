package com.alya.ecommerce_serang.utils

object Constants {
    // API Endpoints
    const val ENDPOINT_SEND_CHAT = "/sendchat"
    const val ENDPOINT_UPDATE_CHAT_STATUS = "/chatstatus"
    const val ENDPOINT_GET_CHAT_DETAIL = "/chatdetail"

    // Shared Preferences
    const val PREF_NAME = "app_preferences"
    const val KEY_USER_ID = "user_id"
    const val KEY_TOKEN = "token"

    // Intent extras
    const val EXTRA_CHAT_ROOM_ID = "chat_room_id"
    const val EXTRA_STORE_ID = "store_id"
    const val EXTRA_PRODUCT_ID = "product_id"
    const val EXTRA_STORE_NAME = "store_name"
    const val EXTRA_PRODUCT_NAME = "product_name"
    const val EXTRA_PRODUCT_PRICE = "product_price"
    const val EXTRA_PRODUCT_IMAGE = "product_image"
    const val EXTRA_PRODUCT_RATING = "product_rating"
    const val EXTRA_STORE_IMAGE = "store_image"
    const val EXTRA_USER_ID = "user_id"
    const val EXTRA_USER_NAME = "user_name"
    const val EXTRA_USER_IMAGE = "user_image"
    const val EXTRA_ATTACH_PRODUCT = "extra_attach_product"



    // Request codes
    const val REQUEST_IMAGE_PICK = 1001
    const val REQUEST_CAMERA = 1002
    const val REQUEST_STORAGE_PERMISSION = 1003

    // Socket.IO events
    const val EVENT_JOIN_ROOM = "joinRoom"
    const val EVENT_NEW_MESSAGE = "new_message"
    const val EVENT_MESSAGE_DELIVERED = "message_delivered"
    const val EVENT_MESSAGE_READ = "message_read"
    const val EVENT_TYPING = "typing"

    // Message status
    const val STATUS_SENT = "sent"
    const val STATUS_DELIVERED = "delivered"
    const val STATUS_READ = "read"
}