package com.glimmer.wechatautoservice.util

import android.content.Context
import android.widget.Toast

/**
 * @author Glimmer
 * 2019/01/28
 */
fun Context.showToast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}