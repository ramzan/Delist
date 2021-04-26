package ca.ramzan.delist.screens

import androidx.fragment.app.Fragment

open class BaseFragment<BINDING_TYPE> : Fragment() {

    protected var mutableBinding: BINDING_TYPE? = null
    protected val binding get() = mutableBinding!!

    override fun onDestroyView() {
        super.onDestroyView()
        mutableBinding = null
    }
}