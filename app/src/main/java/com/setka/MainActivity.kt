package com.setka

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.setka.ui.*
import com.setka.ui.theme.SetkaTheme
import com.setka.viewmodel.ChatViewModel

import com.setka.service.SetkaForegroundService

class MainActivity : ComponentActivity() {

    private val enableBtLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        enableBluetooth()
        startForegroundService()
        val openChatId = intent.getStringExtra("chat_id")

        setContent {
            SetkaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    val vm: ChatViewModel = viewModel()
                    val myUser        by vm.myUser.collectAsState()
                    val isFirstLaunch by vm.isFirstLaunch.collectAsState()

                    var showSplash     by remember { mutableStateOf(true) }
                    var showOnboarding by remember { mutableStateOf(false) }

                    when {
                        // 1. Сплэш
                        showSplash -> {
                            SplashScreen(onFinished = {
                                showSplash = false
                                // Показываем онбординг только при самом первом запуске
                                if (myUser == null || isFirstLaunch) showOnboarding = true
                            })
                        }

                        // 2. Онбординг (только первый раз)
                        showOnboarding -> {
                            OnboardingScreen(onDone = { showOnboarding = false })
                        }

                        // 3. Экран приветствия (ввод никнейма)
                        myUser == null || isFirstLaunch -> {
                            WelcomeScreen(vm = vm, onDone = {})
                        }

                        // 4. Основное приложение
                        else -> {
                            AppNavigation(vm = vm, startChatId = openChatId)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun startForegroundService() {
        val intent = Intent(this, SetkaForegroundService::class.java).apply {
            action = SetkaForegroundService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun enableBluetooth() {
        val btAdapter = (getSystemService(BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        if (btAdapter?.isEnabled == false) {
            enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }

    private fun requestPermissions() {
        val perms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        permissionsLauncher.launch(perms.toTypedArray())
    }
}
