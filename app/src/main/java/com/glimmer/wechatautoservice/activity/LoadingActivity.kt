package com.glimmer.wechatautoservice.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.glimmer.wechatautoservice.R
import kotlinx.android.synthetic.main.activity_loading.*

/**
 * @author Glimmer
 * 2019/01/18
 */
class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        root.postDelayed({
            finish()
        }, 100)
    }

}