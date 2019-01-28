package com.glimmer.wechatautoservice.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.glimmer.wechatautoservice.R
import kotlinx.android.synthetic.main.activity_about.*

/**
 * @author Glimmer
 * 2019/01/23
 * 如何使用
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        ivBack.setOnClickListener { finish() }
    }

}