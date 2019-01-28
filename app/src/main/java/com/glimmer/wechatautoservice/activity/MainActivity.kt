package com.glimmer.wechatautoservice.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.glimmer.wechatautoservice.R
import com.glimmer.wechatautoservice.bean.HBMsgEvent
import com.glimmer.wechatautoservice.config.Constant
import com.glimmer.wechatautoservice.service.NotificationService
import com.glimmer.wechatautoservice.service.WeChatAutoService
import com.glimmer.wechatautoservice.util.AccessibilityUtil
import com.glimmer.wechatautoservice.util.SPUtils
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 * @author Glimmer
 * 2019/01/28
 * 主页
 */
class MainActivity : AppCompatActivity() {
    private lateinit var mContext: Context
    private var mStringBuilder: StringBuilder = java.lang.StringBuilder()
    private var mHBRecord: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = this
        EventBus.getDefault().register(this)

        // 系统辅助服务
        tvStartAssistService.setOnClickListener { AccessibilityUtil.jumpToSetting(mContext) }
        // 设置
        tvSetting.setOnClickListener { startActivity(Intent(mContext, SettingActivity::class.java)) }
        // 清除记录
        tvClearMoneyRecord.setOnClickListener {
            AlertDialog.Builder(mContext)
                    .setNegativeButton("取消", null)
                    .setMessage("确定删除所有红包记录吗？")
                    .setPositiveButton("确定") { _, _ ->
                        SPUtils.getInstance().put(Constant.SP_KEY_HB, "")
                        tvClearMoneyRecord.visibility = View.GONE
                        mStringBuilder.replace(0, mStringBuilder.length, "")
                        tvMoneyRecord.text = ""
                    }
                    .create()
                    .show()
        }

        // 启动服务
        startService(Intent(this, NotificationService::class.java))
        // 初始化红包记录
        initRecordData()
    }

    private fun initRecordData() {
        mStringBuilder = StringBuilder()
        mHBRecord = SPUtils.getInstance().getString(Constant.SP_KEY_HB, "")
        if (!mHBRecord.isEmpty()) {
            val recordArr = mHBRecord.split(Constant.SPLIT_COMMA.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (recordArr.size >= 2) {
                for (index in recordArr.indices) {
                    if (index % 2 == 0) {
                        mStringBuilder.append(recordArr[index]).append("----->")
                    } else {
                        mStringBuilder.append(recordArr[index]).append("\n\n")
                    }
                }
            }
            tvMoneyRecord.text = mStringBuilder.toString()
        }

        if (mStringBuilder.isNotEmpty()) {
            tvClearMoneyRecord.visibility = View.VISIBLE
        } else {
            tvClearMoneyRecord.visibility = View.GONE
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onHBMsgEvent(event: HBMsgEvent) {
        saveRecordToLocal(event)

        val lastRecord = mStringBuilder.toString()
        mStringBuilder.delete(0, lastRecord.length)
        mStringBuilder.append(event.content)
                .append("----->")
                .append(event.money)
                .append("\n\n")
                .append(lastRecord)

        tvMoneyRecord.text = mStringBuilder.toString()
        tvClearMoneyRecord.visibility = View.VISIBLE
    }

    /**
     * 保存红包记录到本地
     *
     * @param event 红包信息
     */
    private fun saveRecordToLocal(event: HBMsgEvent) {
        var endStr = ""
        if (!mHBRecord.isEmpty()) {
            endStr = Constant.SPLIT_COMMA + mHBRecord
        }
        mHBRecord = event.content + Constant.SPLIT_COMMA + event.money + endStr
        SPUtils.getInstance().put(Constant.SP_KEY_HB, mHBRecord)
    }

    override fun onResume() {
        super.onResume()
        // 是否开启辅助服务
        val isOpen = AccessibilityUtil.isSettingOpen(WeChatAutoService::class.java, mContext)
        if (!isOpen) {
            AlertDialog.Builder(mContext)
                    .setNegativeButton("取消", null)
                    .setMessage("红包服务已被关闭，请重新打开")
                    .setPositiveButton("确定") { dialog, _ ->
                        dialog.dismiss()
                        tvStartAssistService.performClick()
                    }
                    .create()
                    .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addCategory(Intent.CATEGORY_HOME)
        startActivity(intent)
    }
}