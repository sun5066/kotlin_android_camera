package github.sun5066.camera

import android.os.Bundle
import android.os.PersistableBundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class BaseActivity<DB : ViewDataBinding> : AppCompatActivity() {

    lateinit var mBinding: DB

    @LayoutRes
    abstract fun getLayoutResourceId(): Int

    abstract fun initDataBinding()

    abstract fun initView()

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        mBinding = DataBindingUtil.setContentView(this, getLayoutResourceId())

        this.initDataBinding()
        this.initView()
    }
}