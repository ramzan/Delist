package ca.ramzan.delist.screens

import androidx.fragment.app.Fragment

open class BaseFragment<BINDING_TYPE> : Fragment() {

    private var _binding: BINDING_TYPE? = null
    protected val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setUpBinding(bindingType: BINDING_TYPE) {
        _binding = bindingType
    }
}