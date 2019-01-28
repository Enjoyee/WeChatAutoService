package com.glimmer.wechatautoservice.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.glimmer.wechatautoservice.config.Constant
import com.glimmer.wechatautoservice.R
import com.glimmer.wechatautoservice.util.SPUtils
import kotlinx.android.synthetic.main.activity_setting_reply.*

/**
 * @author Glimmer
 * 2019/01/23
 * 设置回复语
 */
class SettingReplyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_reply)
        ivBack.setOnClickListener { finish() }
        val replyContent = SPUtils.getInstance().getString(Constant.SP_KEY_HB_REPLY, Constant.DEFAULT_REPLY_CONTENT)
        etReply.setText(replyContent)
        etReply.setSelection(etReply.text.length)
        tvSave.setOnClickListener {
            SPUtils.getInstance().put(Constant.SP_KEY_HB_REPLY, etReply.text.toString())
            finish()
        }
    }

}