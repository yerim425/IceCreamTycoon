package com.example.risingcamp4

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.risingcamp4.databinding.ActivityStartBinding




class StartActivity : AppCompatActivity() {
    lateinit var binding: ActivityStartBinding
    lateinit var bgm: MediaPlayer
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        bgm = MediaPlayer.create(this, R.raw.bgm_start)
        bgm.isLooping = true


        sharedPreferences = getSharedPreferences("gameData", MODE_PRIVATE)
        var editor = sharedPreferences.edit()

        var day = sharedPreferences.getInt("day", 0)
        if(day == 0){
            day = 1
            editor.putInt("day", 1)
        }
        editor.commit()
        binding.tvDay.setText(day.toString()+"일차") // 영업 일차 설정

        // 게임 화면으로 넘어감
        binding.btnStart.setOnClickListener {

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        bgm.start()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        bgm.pause()
    }

    override fun onStop() {
        super.onStop()
        bgm.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        bgm.stop()
    }
}