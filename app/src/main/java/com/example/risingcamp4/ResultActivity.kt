package com.example.risingcamp4

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.risingcamp4.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    lateinit var binding: ActivityResultBinding
    lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        var salesVolume = intent.getIntExtra("salesVolume", -1)
        var originalMoney = intent.getIntExtra("originalMoney", -1)
        var ingredientMoney = intent.getIntExtra("ingredientMoney", -1)
        var mistakeMoney = intent.getIntExtra("mistakeMoney", -1)
        var todayMoney = originalMoney - ingredientMoney

        sharedPreferences = getSharedPreferences("gameData", MODE_PRIVATE)
        var editor = sharedPreferences.edit()

        var totalMoney = sharedPreferences.getInt("money", 0)
        totalMoney += todayMoney
        editor.putInt("money", totalMoney) // 지금까지 영업한 날의 총 수익 저장

        var day = sharedPreferences.getInt("day", 0)
        day += 1
        editor.putInt("day", day)

        editor.commit()

        with(binding){
            inputSalesVolume.setText(salesVolume.toString())
            inputSalesMoney.setText(originalMoney.toString())
            inputIngredientMoney.setText("-"+ingredientMoney.toString())
            inputMistakeMoney.setText("-"+mistakeMoney.toString())
            inputTodayMoney.setText("+"+todayMoney.toString())


            btnOk.setOnClickListener {
                startActivity(Intent(applicationContext, StartActivity::class.java))
                finish()
            }
        }
    }
}