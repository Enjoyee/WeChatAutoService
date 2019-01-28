package com.glimmer.wechatautoservice.activity

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.glimmer.wechatautoservice.R
import kotlinx.android.synthetic.main.activity_setting.*

/**
 * @author Glimmer
 * 2019/01/18
 * 应用设置
 */
class SettingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)
        ivBack.setOnClickListener { finish() }
        tvUsage.setOnClickListener { startActivity(Intent(this, AboutActivity::class.java)) }
        tvReply.setOnClickListener { startActivity(Intent(this, SettingReplyActivity::class.java)) }
    }

}