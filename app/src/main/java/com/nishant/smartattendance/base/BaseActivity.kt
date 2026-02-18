package com.nishant.smartattendance.base

import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    protected open fun setupView() {}
    protected open fun setupObservers() {}
    protected open fun setupListeners() {}

}
