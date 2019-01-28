package com.glimmer.wechatautoservice.service

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.glimmer.wechatautoservice.activity.LoadingActivity
import com.glimmer.wechatautoservice.activity.MainActivity
import com.glimmer.wechatautoservice.bean.HBMsgEvent
import com.glimmer.wechatautoservice.config.Constant
import com.glimmer.wechatautoservice.config.Constant.TAG
import com.glimmer.wechatautoservice.util.SPUtils
import com.glimmer.wechatautoservice.util.showToast
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import org.greenrobot.eventbus.EventBus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * @author Glimmer
 * 2019/01/17
 * 微信自动服务
 */
class WeChatAutoService : AccessibilityService() {
    companion object {
        /**
         * 微信包名
         */
        private const val WECHAT_PACKAGE_NAME = "com.tencent.mm"
        /**
         * 微信消息列表详情
         */
        private const val WECHAT_MSG_LIST_CLASS_NAME = "com.tencent.mm.ui.LauncherUI"
        /**
         * 微信红包打开
         */
        private const val WECHAT_HB_CLASS_NAME = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI"
        /**
         * 微信红包详情
         */
        private const val WECHAT_HB_DETAIL_CLASS_NAME = "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI"

        /**
         * 实名认证
         */
        private const val CLASS_NAME_REAL_NAME = "com.tencent.mm.plugin.wallet_core.id_verify.SwitchRealnameVerifyModeUI"
        /**
         * 按钮
         */
        private const val BTN_CLASS_NAME = "android.widget.Button"
        /**
         * 线性布局
         */
        private const val LINEAR_LAYOUT_CLASS_NAME = "android.widget.LinearLayout"
        /**
         * 输入框
         */
        private const val EDIT_TEXT_CLASS_NAME = "android.widget.EditText"

        /**
         * 红包信息
         */
        private const val HB_NOTIFICATION = "[微信红包]"
        /**
         * 发送按钮中文文字
         */
        private const val TXT_CN_SEND = "发送"
        /**
         * 发送按钮英文文字
         */
        private const val TXT_ENG_SEND = "Send"

        /**
         * 领取红包
         */
        private const val GET_HB_PACKET = "微信红包"

        /**
         * 红包详情页面抢到的红包额度id
         */
        private const val HB_DETAIL_MONEY = "com.tencent.mm:id/cqv"
        /**
         * 谁发的红包id
         */
        private const val HB_DETAIL_NAME = "com.tencent.mm:id/cqr"
    }

    /**
     * 用于存储微信开红包按钮使用过的id，微信几乎每次版本更新都会修改此button的id
     */
    private val hbBtIdList = Arrays.asList("com.tencent.mm:id/cv0", "bjj", "bi3", "brt", "ci3", "c4j")

    /**
     * 是否是自动打开的通知消息，手动点击进入则不进行抢红包操作
     */
    private var mIsAutoOpenNotification: Boolean = false
    private var mIsSwitchLoadingActivity = true
    private var mIsShowToast = true
    private var mHBMsgEvent: HBMsgEvent? = null
    private var mLoopCount = 3
    private lateinit var dispose: Disposable
    private var mHbBtnNodeInfoList: List<AccessibilityNodeInfo>? = mutableListOf()

    @SuppressLint("CheckResult")
    override fun onServiceConnected() {
        super.onServiceConnected()
        mHBMsgEvent = HBMsgEvent()
        showToast("微信自动抢红包服务开启")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val nodeInfo = event?.source
        val packageName = event?.packageName
        Log.e(TAG, "1--->packageName: $packageName")
        if (TextUtils.equals(packageName, WECHAT_PACKAGE_NAME)) {
            val className = event?.className
            Log.e(TAG, "2--->className: $className")
            if (mIsShowToast) {
                mIsShowToast = false
                showToast("微信需要退到后台才可以自动抢红包")
            }
            className?.apply {
                when {
                    //通知栏事件
                    event.parcelableData != null -> {
                        openNotification(event)
                    }

                    // 打开红包
                    WECHAT_MSG_LIST_CLASS_NAME.contentEquals(className) -> {
                        Handler().postDelayed({
                            if (mIsAutoOpenNotification) {
                                waitLoopToLoadFun(openHBMsg(rootInActiveWindow))
                            }
                        }, 100)
                    }

                    // 点击抢红包
                    WECHAT_HB_CLASS_NAME.contentEquals(className) -> {
                        Handler().postDelayed({
                            if (mIsAutoOpenNotification) {
                                getHB(nodeInfo)
                            }
                        }, 100)
                    }

                    // 红包详情
                    WECHAT_HB_DETAIL_CLASS_NAME.contentEquals(className) -> {
                        Handler().postDelayed({
                            // 查找抢到了多少钱
                            findMoneyAmount(nodeInfo)
                            // 返回跟回复消息
                            goBackAndReply()
                        }, 100)
                    }

                    // 非实名
                    CLASS_NAME_REAL_NAME.contentEquals(className) -> {
                        backToWeChatMainAndAuto()
                    }
                }
            }
        } else if (packageName?.toString()?.endsWith(".launcher") == true) {
            mIsShowToast = true
            Log.e(TAG, "重置吐司显示控制")
        }
    }

    /**
     * 返回微信首页，回到自动抢红包app
     */
    private fun backToWeChatMainAndAuto(): () -> Unit {
        return {
            while (mLoopCount > 0) {
                sleepTime(100)
                goBack()
                mLoopCount--
                Log.e(TAG, "里面循环数目：$mLoopCount")
            }
            Log.e(TAG, "外边循环数目：$mLoopCount")
            mLoopCount = 3
            mIsShowToast = true
            // 回到抢红包页面
            backToAutoServiceActivity()
        }
    }

    /**
     * 查找抢到的红包金额
     *
     * @param nodeInfo 节点
     */
    private fun findMoneyAmount(nodeInfo: AccessibilityNodeInfo?) {
        var result = false
        nodeInfo?.apply {
            val nameNodeList = findAccessibilityNodeInfosByViewId(HB_DETAIL_NAME)
            val moneyNodeList = findAccessibilityNodeInfosByViewId(HB_DETAIL_MONEY)
            var hbName = ""
            var hbMoney = ""
            nameNodeList?.apply {
                if (this.isNotEmpty()) {
                    hbName = "${this[0].text}"
                }
            }
            moneyNodeList?.apply {
                if (this.isNotEmpty()) {
                    hbMoney = "${this[0].text}元"
                }
            }
            mHBMsgEvent?.content = getCurrentTime()
            mHBMsgEvent?.money = "抢到了${hbName}发的红包，$hbMoney"
            result = hbName.isNotEmpty() && hbMoney.isNotEmpty()
        }
        Log.e(TAG, "findMoneyAmount: " + mHBMsgEvent.toString())
        if (result) {
            // 发送通知界面
            mHBMsgEvent?.apply { EventBus.getDefault().post(this) }
        }
    }

    private fun getCurrentTime(): String = SimpleDateFormat("MM月dd日HH:mm分", Locale.CHINA).format(Date())

    /**
     * 返回并且回复消息
     */
    private fun goBackAndReply() {
        // 返回
        sleepTime(200)
        goBack()
        sleepTime(300)
        val replyContent = SPUtils.getInstance().getString(Constant.SP_KEY_HB_REPLY, Constant.DEFAULT_REPLY_CONTENT)
        waitLoopToLoadFun(findReplyViewAndSendMsg(rootInActiveWindow, replyContent), backToAutoServiceActivity())
    }

    /**
     * 回到抢红包页面
     */
    private fun backToAutoServiceActivity(): () -> Unit {
        return {
            val hbIntent = Intent(baseContext, MainActivity::class.java)
            hbIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(hbIntent)
            mIsSwitchLoadingActivity = true
            mIsShowToast = true
            Log.e(TAG, "回到抢红包首页重置吐司显示控制")
        }
    }

    /**
     * 延时
     *
     * @param millis 时间
     */
    private fun sleepTime(millis: Long) {
        try {
            // 延时一下，避免过快
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    /**
     * 打开通知栏消息
     *
     * @param event 事件
     */
    private fun openNotification(event: AccessibilityEvent) {
        if (event.parcelableData != null) {
            val msgList = event.text
            if (!msgList.isEmpty()) {
                for (msg in msgList) {
                    val text = msg.toString()
                    Log.e(TAG, "3--->消息：$text")
                    // 是红包消息
                    if (text.contains(HB_NOTIFICATION)) {
                        mIsAutoOpenNotification = true
                        //将通知栏消息打开
                        val notification = event.parcelableData as Notification
                        val pendingIntent = notification.contentIntent
                        try {
                            pendingIntent.send()
                        } catch (e: PendingIntent.CanceledException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    /**
     * 打开红包消息
     *
     * @param nodeInfo 节点信息
     */
    private fun openHBMsg(nodeInfo: AccessibilityNodeInfo?): () -> Boolean {
        if (nodeInfo == null) {
            Log.e(TAG, "窗口为空")
            return { false }
        }

        val msgList = nodeInfo.findAccessibilityNodeInfosByText(GET_HB_PACKET)
        if (!msgList.isEmpty()) {
            for (index in msgList.indices.reversed()) {
                val parent = msgList[index].parent
                if (parent != null) {
                    if (LINEAR_LAYOUT_CLASS_NAME.contentEquals(parent.className)) {
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        return { true }
                    }
                }
            }
        }

        return { false }
    }

    /**
     * 抢红包
     */
    private fun getHB(rootNodeInfo: AccessibilityNodeInfo?) {
        // 点击抢红包
        if (rootNodeInfo == null) {
            Log.e(TAG, "窗口为空")
            return
        }

        if (mIsSwitchLoadingActivity) {
            mIsSwitchLoadingActivity = false
            startToMaskView()
            Log.e(TAG, "加载遮罩层...")
        } else {
            waitLoopToLoadFun(findHBOpenBtnAndClick(rootNodeInfo), backToWeChatMainAndAuto())
            Log.e(TAG, "开始等待循环获取打开红包的按钮...")
        }
    }

    /**
     * 找到开红包的按钮点击
     */
    private fun findHBOpenBtnAndClick(rootNodeInfo: AccessibilityNodeInfo): () -> Boolean {
        var isSuccess = false
        for (id in hbBtIdList) {
            mHbBtnNodeInfoList = rootNodeInfo.findAccessibilityNodeInfosByViewId(id)
            Log.e(TAG, "getHB id: $id，mIsAutoOpenNotification = $mIsAutoOpenNotification")
            if (mHbBtnNodeInfoList != null && mHbBtnNodeInfoList!!.isNotEmpty() && mIsAutoOpenNotification) {
                isSuccess = true
                Log.e(TAG, "getHB: " + mHbBtnNodeInfoList?.size)
                mHbBtnNodeInfoList!![0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                mIsAutoOpenNotification = false
            }
        }
        return { isSuccess }
    }

    private fun waitLoopToLoadFun(subscribeFun: () -> Boolean, finallyFun: () -> Unit) {
        val timeCount = 2L
        dispose = Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .map { timeCount - it }
                .take(timeCount)
                .doFinally {
                    if (!dispose.isDisposed) {
                        finallyFun()
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (subscribeFun()) {
                        dispose.dispose()
                    }
                }
    }

    private fun waitLoopToLoadFun(subscribeFun: () -> Boolean) {
        val timeCount = 2L
        dispose = Observable.interval(0, 200, TimeUnit.MILLISECONDS)
                .map { timeCount - it }
                .take(timeCount)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    if (subscribeFun()) {
                        dispose.dispose()
                    }
                }
    }

    /**
     * 加载遮罩层
     */
    private fun startToMaskView() {
        val intent = Intent(this, LoadingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    /**
     * 查找回复输入框
     *
     * @param nodeInfo 节点
     * @param msg      回复的消息
     */
    private fun findReplyViewAndSendMsg(nodeInfo: AccessibilityNodeInfo?, msg: String): () -> Boolean {
        var result = false
        if (nodeInfo != null) {
            val childCount = nodeInfo.childCount
            for (index in 0 until childCount) {
                val childNodeInfo = nodeInfo.getChild(index)
                if (childNodeInfo != null) {
                    val className = childNodeInfo.className
                    Log.e(TAG, "findReplyViewAndSendMsg: $className")
                    if (EDIT_TEXT_CLASS_NAME.contentEquals(className)) {
                        Log.e(TAG, "找到了回复输入框")
                        inputContent(childNodeInfo, msg)
                        sendWeChatMsg(rootInActiveWindow)
                        result = true
                    } else {
                        findReplyViewAndSendMsg(childNodeInfo, msg)
                    }
                }
            }
        }

        return { result }
    }

    /**
     * 输入微信消息
     *
     * @param nodeInfo nodeInfo
     * @param content  内容
     */
    private fun inputContent(nodeInfo: AccessibilityNodeInfo, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, content)
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        } else {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("weChatAutoService", content)
            clipboard?.apply {
                clipboard.primaryClip = clip
            }
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_PASTE)
        }
    }

    /**
     * 发送微信消息
     *
     * @param rootNodeInfo 窗口信息
     */
    private fun sendWeChatMsg(rootNodeInfo: AccessibilityNodeInfo?) {
        if (rootNodeInfo != null) {
            val cnNodeInfoList = rootNodeInfo.findAccessibilityNodeInfosByText(TXT_CN_SEND)
            if (cnNodeInfoList != null && cnNodeInfoList.size > 0) {
                for (nodeInfo in cnNodeInfoList) {
                    if (nodeInfo.className == BTN_CLASS_NAME && nodeInfo.isEnabled) {
                        nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }
            } else {
                val engNodeInfoList = rootNodeInfo.findAccessibilityNodeInfosByText(TXT_ENG_SEND)
                if (engNodeInfoList != null && engNodeInfoList.size > 0) {
                    for (nodeInfo in engNodeInfoList) {
                        if (nodeInfo.className == BTN_CLASS_NAME && nodeInfo.isEnabled) {
                            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                    }
                }
            }
            sleepTime(300)
            goBack()
        }
    }

    /**
     * 返回上一级
     */
    private fun goBack() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    override fun onInterrupt() {
        showToast("微信自动服务中断")
    }

}