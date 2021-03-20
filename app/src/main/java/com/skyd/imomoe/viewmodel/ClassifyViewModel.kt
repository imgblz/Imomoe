package com.skyd.imomoe.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.skyd.imomoe.App
import com.skyd.imomoe.R
import com.skyd.imomoe.bean.AnimeCoverBean
import com.skyd.imomoe.bean.ClassifyBean
import com.skyd.imomoe.bean.PageNumberBean
import com.skyd.imomoe.config.Api
import com.skyd.imomoe.util.JsoupUtil
import com.skyd.imomoe.util.ParseHtmlUtil.parseLpic
import com.skyd.imomoe.util.ParseHtmlUtil.parseNextPages
import com.skyd.imomoe.util.ParseHtmlUtil.parseTers
import com.skyd.imomoe.util.Util.showToastOnThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.select.Elements
import java.lang.Exception
import java.util.*


class ClassifyViewModel : ViewModel() {
    var isRequesting = false
    var classifyTabList: MutableList<ClassifyBean> = ArrayList()        //上方分类数据
    var mldClassifyTabList: MutableLiveData<Boolean> = MutableLiveData()
    var classifyList: MutableList<AnimeCoverBean> = ArrayList()       //下方tv数据
    var mldClassifyList: MutableLiveData<Int> = MutableLiveData()       // value：-1错误；0重新获取；1刷新
    var pageNumberBean: PageNumberBean? = null
    var newPageIndex: Pair<Int, Int>? = null

    fun getClassifyTabData() {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val document = JsoupUtil.getDocument(Api.MAIN_URL + "/a/")
                val areaElements: Elements = document.getElementsByClass("area")
                classifyTabList.clear()
                for (i in areaElements.indices) {
                    val areaChildren: Elements = areaElements[i].children()
                    for (j in areaChildren.indices) {
                        when (areaChildren[j].className()) {
                            "ters" -> {
                                classifyTabList.addAll(parseTers(areaChildren[j]))
                            }
                        }
                    }
                }
                mldClassifyTabList.postValue(true)
            } catch (e: Exception) {
                classifyTabList.clear()
                mldClassifyTabList.postValue(false)
                e.printStackTrace()
                (App.context.getString(R.string.get_data_failed) + "\n" + e.message).showToastOnThread()
            }
        }
    }

    fun getClassifyData(partUrl: String, isRefresh: Boolean = true) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                if (isRequesting) return@launch
                isRequesting = true
                pageNumberBean = null
                val url = Api.MAIN_URL + partUrl
                val document = JsoupUtil.getDocument(url)
                val areaElements: Elements = document.getElementsByClass("area")
                if (isRefresh) classifyList.clear()
                val positionStart = classifyList.size
                for (i in areaElements.indices) {
                    val areaChildren: Elements = areaElements[i].children()
                    for (j in areaChildren.indices) {
                        when (areaChildren[j].className()) {
                            "fire l" -> {
                                val fireLChildren: Elements = areaChildren[j].children()
                                for (k in fireLChildren.indices) {
                                    when (fireLChildren[k].className()) {
                                        "lpic" -> {
                                            classifyList.addAll(parseLpic(fireLChildren[k], url))
                                        }
                                        "pages" -> {
                                            pageNumberBean = parseNextPages(fireLChildren[k])
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                newPageIndex = Pair(positionStart, classifyList.size - positionStart)
                mldClassifyList.postValue(if (isRefresh) 0 else 1)
            } catch (e: Exception) {
                pageNumberBean = null
                classifyList.clear()
                mldClassifyList.postValue(-1)
                e.printStackTrace()
                (App.context.getString(R.string.get_data_failed) + "\n" + e.message).showToastOnThread()
            }
        }
    }

    companion object {
        const val TAG = "ClassifyViewModel"
    }
}