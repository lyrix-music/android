package `in`.srev.lyrix.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import `in`.srev.lyrix.data.LoginRepository
import `in`.srev.lyrix.data.Result


class LoginViewModel(private val loginRepository: LoginRepository) : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(username: String, password: String) {
        // can be launched in a separate asynchronous job

    }

    fun loginDataChanged(username: String, password: String) {
        // TODO
    }

    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        // TODO
        return true
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        // TODO
        return true
    }
}