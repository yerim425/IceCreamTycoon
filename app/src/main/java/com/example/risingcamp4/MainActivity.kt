package com.example.risingcamp4

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.risingcamp4.databinding.ActivityMainBinding
import java.util.*




class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    lateinit var bgm: MediaPlayer
    var gameTime = 120 // 2분
    val handler = Handler(Looper.getMainLooper())
    lateinit var sharedPreferences: SharedPreferences

    lateinit var timeTextView: TextView // 시간 textview
    lateinit var moneyTextView: TextView // 돈 textview

    lateinit var customerLayout: ArrayList<ConstraintLayout>        // 각 손님의 layout
    lateinit var customerCharacterIv: ArrayList<ImageView>            // 각 손님의 캐릭터 ImageView
    lateinit var customerOrderIv: ArrayList<ArrayList<ImageView>>   // 각 손님의 주문아이스크림 ImageView
    lateinit var customerResultIv: ArrayList<ImageView>               // 각 손님의 반응 ImageView
    lateinit var customerPayTv: ArrayList<TextView>                   // 각 손님이 지불한 금액 TextView
    lateinit var finishLayout: FrameLayout                          // 게임 종료 후 띄울 finish 화면

    // 손님 캐릭터 이미지
    var customerSmileList = listOf(R.drawable.smile0, R.drawable.smile1, R.drawable.smile2, R.drawable.smile3, R.drawable.smile4, R.drawable.smile5)
    var customerSadList = listOf(R.drawable.sad0, R.drawable.sad1, R.drawable.sad2, R.drawable.sad3, R.drawable.sad4, R.drawable.sad5)
    var customerAngryList = listOf(R.drawable.angry0, R.drawable.angry1, R.drawable.angry2, R.drawable.angry3, R.drawable.angry4, R.drawable.angry5)
    var customerLaughList = listOf(R.drawable.laugh0, R.drawable.laugh1, R.drawable.laugh2, R.drawable.laugh3, R.drawable.laugh4, R.drawable.laugh5)

    // (캐릭터 이미지 사용 여부(on, off), 사용한 자리(0, 1, 2)) : 캐릭터 중복 없애기 위함
    var characterImageStatus = arrayListOf(
        arrayListOf("off", "-1"), arrayListOf("off", "-1"), arrayListOf("off", "-1"),
        arrayListOf("off", "-1"), arrayListOf("off", "-1"), arrayListOf("off", "-1"),
    )

    // 아이스크림 종류별 이미지 : 아이스크림 통 클릭 리스너에서 사용
    var iceCreamList = listOf(R.drawable.ice_vanilla, R.drawable.ice_choco, R.drawable.ice_strawberry,
        R.drawable.ice_mint_choco, R.drawable.ice_blueberry)

    // 아이스크림을 맞게 제작했는지 비교할 때 사용할 리스트
    var customerOrderInt = arrayListOf(arrayListOf(-1, -1), arrayListOf(-1, -1), arrayListOf(-1, -1)) // 손님이 주문한 아이스크림 이미지 관리(1단, 2단)
    var makingOrderInt = arrayListOf(-1, -1) // 제작 아이스크림 이미지 관리(1단, 2단)


    var ingredientMoney = 0     // 지출될 재료비
    var mistakeMoney = 0        // 지출된 실수 보상 비용
    var originalMoney = 0       // 재료비, 실수 고려안했을 때의 총 금액
    var totalMoney = 0          // 화면에 표시할 총 수익금

    val COST_ICECREAM = 10      // 1단 아이스크림 판매값
    val COST_ICECREAM2 = 15     // 2단 아이스크림 판매값

    val COST_ICE = 3            // 아이스크림 한 덩이 재료값
    val COST_CONE = 1           // 콘 하나 재료값
    val COST_MISMAKING = 3      // 실수 보상 비용

    var makingBooleanList = arrayListOf(false, false, false) // 아이스크림 재료 사용 여부(cone, ice1, ice2)

    var salesVolume = 0 // 판매수량

    var iceResult = -1 // 아이스크림 전달 후 결과

    var gameOver = false // 손님 스레드 멈추기

    var customerThreadActive = true // 손님 쓰레드의 동작 제어


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // bgm 설정
        bgm = MediaPlayer.create(this, R.raw.bgm_game)
        bgm.isLooping = true


        // sharedPreferences -> 현재까지의 영업 수익금 가져오기
        sharedPreferences = getSharedPreferences("gameData", MODE_PRIVATE)
        totalMoney = sharedPreferences.getInt("money", 0)

        if(totalMoney == 0){ // 영업 첫날이라면 0원으로 초기화
            var editor = sharedPreferences.edit()
            editor.putInt("money", 0)
            editor.commit()
        }


        // 클릭 리스너
        with(binding) {

            // 콘 담겨있는 통 클릭 리스너
            ivCones.setOnClickListener {
                if (makingBooleanList[0] == false) { // 콘 셋팅이 안되어 있다면
                    ivMakingCone.visibility = View.VISIBLE // 콘 셋팅
                    makingBooleanList[0] = true
                }
            }
            // 아이스크림 통 클릭 리스너
            ivVanilla.setOnClickListener { makingIceCream(R.drawable.ice_vanilla) }
            ivChoco.setOnClickListener { makingIceCream(R.drawable.ice_choco) }
            ivStrowberry.setOnClickListener { makingIceCream(R.drawable.ice_strawberry) }
            ivMintChoco.setOnClickListener { makingIceCream(R.drawable.ice_mint_choco) }
            ivBlueberry.setOnClickListener { makingIceCream(R.drawable.ice_blueberry) }

            // 휴지통 클릭 리스너너
            ivTrash.setOnClickListener {
                if (makingBooleanList[0] == true) { // 콘이 셋팅 되어있다면
                    ingredientMoney += COST_CONE // 콘 재료비 +
                    makingBooleanList[0] = false
                    ivMakingCone.visibility = View.GONE

                    if (makingBooleanList[1] == true) { // 1단 아이스크림이 셋팅 되어있다면
                        ingredientMoney += COST_ICE // 아이스크림 재료값 +
                        makingBooleanList[1] = false
                        ivMakingIce1.visibility = View.GONE

                        if (makingBooleanList[2] == true) { // 2단 아이스크림이 셋팅 되어있다면
                            ingredientMoney += COST_ICE
                            makingBooleanList[2] = false
                            ivMakingIce2.visibility = View.GONE

                        }
                    }
                }
            }

            // 손님 클릭 리스너(손님에게 아이스크림 전달)
            ivCustomer1.setOnClickListener {GivingIceCream(0, ivOrder1Ice1, ivOrder1Ice2)}
            ivCustomer2.setOnClickListener {GivingIceCream(1, ivOrder2Ice1, ivOrder2Ice2)}
            ivCustomer3.setOnClickListener {GivingIceCream(2, ivOrder3Ice1, ivOrder3Ice2)}

        }

        initScreen()


    }

    fun initScreen(){ // 게임 화면 초기화

        // 손님 초기화
        with(binding) {


            layoutCustomer1.visibility = View.INVISIBLE
            ivOrder1Ice2.visibility = View.GONE

            layoutCustomer2.visibility = View.INVISIBLE
            ivOrder2Ice2.visibility = View.GONE

            layoutCustomer3.visibility = View.INVISIBLE
            ivOrder3Ice2.visibility = View.GONE

            // 손님의 레이아웃
            customerLayout = arrayListOf(layoutCustomer1, layoutCustomer2, layoutCustomer3)
            for (i in 0 until customerLayout.size)
                customerLayout[i].setTag("off")

            // 손님의 캐릭터 이미지 뷰
            customerCharacterIv = arrayListOf(ivCustomer1, ivCustomer2, ivCustomer3)

            // 손님의 주문 아이스크림 이미지 뷰
            customerOrderIv = arrayListOf(arrayListOf(ivOrder1Ice1, ivOrder1Ice2), arrayListOf(ivOrder2Ice1, ivOrder2Ice2), arrayListOf(ivOrder3Ice1, ivOrder3Ice2))

            // 아이스크림 전달 받고 나서 손님의 반응(O/X) 이미지 뷰
            customerResultIv = arrayListOf(ivResult1, ivResult2, ivResult3)
            ivResult1.visibility = View.GONE
            ivResult2.visibility = View.GONE
            ivResult3.visibility = View.GONE

            // 손님이 지불한 돈 텍스트 뷰
            customerPayTv = arrayListOf(tvPay1, tvPay2, tvPay3)
            tvPay1.visibility = View.GONE
            tvPay2.visibility = View.GONE
            tvPay3.visibility = View.GONE

            // 제작 아이스크림 이미지 뷰
            binding.ivMakingCone.visibility = View.GONE
            binding.ivMakingIce1.visibility = View.GONE
            binding.ivMakingIce2.visibility = View.GONE

            // 시간
            timeTextView = binding.timeLimit
            timeLimit.setText("02:00")
            timeLimit.setTextColor(Color.BLACK)

            // 코인
            moneyTextView = binding.tvMoney
            moneyTextView.setText(totalMoney.toString()) // totalMoney-> sharedPreference에서 가져옴

            // finish 텍스트 화면
            finishLayout = binding.showFinish
        }
    }

    override fun onResume() {
        super.onResume()
        bgm.start()


        Thread{ // 타이머

            Thread.sleep(1000)

            for(i in 0 until 3){
                CustomerThread(i).start() // 손님 스레드 시작
            }

            while(true){
                Thread.sleep(1000) // 1초 마다

                if(gameTime == 0){ // 게임이 끝났다면

                    gameOver = true // 손님 쓰레드 break

                    handler.post{ // finish 텍스트 띄움
                        binding.showFinish.setBackgroundResource(R.drawable.count_finish)
                        binding.showFinish.visibility = View.VISIBLE
                    }
                    Thread.sleep(500)
                    break
                }


                gameTime-- // 1초 줄어듦
                var min = String.format("%02d", gameTime/60) // 분단위
                var sec = String.format("%02d", gameTime%60) // 초단위

                handler.post { // 시간 문자열 셋팅
                    if(min.toInt() == 0 && sec.toInt() == 10){
                        // 남은 시간 10초부터는 text color를 red로 변경
                        binding.timeLimit.setTextColor(Color.RED)
                    }
                    binding.timeLimit.setText(min+":"+sec)
                }
            }

            // 게임이 끝나면(2분) 결과 화면으로 넘어감
            var intent = Intent(applicationContext, ResultActivity::class.java)
            intent.putExtra("salesVolume", salesVolume) // 판매수
            intent.putExtra("originalMoney", originalMoney) // 판매액
            intent.putExtra("ingredientMoney", ingredientMoney) // 총 재료비
            intent.putExtra("mistakeMoney", mistakeMoney) // 총 실수비
            startActivity(intent)
            finish()

        }.start()

    }

    // 아이스크림 만들기
    fun makingIceCream(ice: Int) {
        if (makingBooleanList[0] == true) { //콘이 준비된 상태라면
            if (makingBooleanList[1] == false) { // 1단이 비어있다면
                makingBooleanList[1] = true // 1단 부터 채움
                binding.ivMakingIce1.setImageResource(ice)
                binding.ivMakingIce1.visibility = View.VISIBLE
                makingOrderInt[0] = ice // 쌓은 아이스크림의 이미지 저장
            }
            else if (makingBooleanList[2] == false) {// 1단 채워짐, 2단 비어있음
                makingBooleanList[2] = true // 2단 채움
                binding.ivMakingIce2.setImageResource(ice)
                binding.ivMakingIce2.visibility = View.VISIBLE
                makingOrderInt[1] = ice // 쌓은 아이스크림의 이미지 저장
            }
        }
    }

    // 아이스크림 주문대로 만들었는지 확인
    fun GivingIceCream(idx: Int, orderIce1: ImageView, orderIce2: ImageView) {

        if (makingBooleanList[1] == true) { // 아이스크림을 쌓았을 때만 확인

            if(customerLayout[idx].getTag().toString() == "angry"){ // 손님이 화난 상태라면 아이스크림 안 받음
                if(makingBooleanList[2] == false){
                    ingredientMoney += (COST_ICE + COST_CONE) // 1단 아이스크림 재료값 추가
                }else{
                    ingredientMoney += (COST_ICE*2 + COST_CONE) // 2단 아이스크림 재료값 추가
                }
            }else{ // Tag = "waiting"
                var pay = 0
                if(customerOrderInt[idx][1] == -1 && makingOrderInt[1] == -1){ // 1단 주문, 1단 제작
                    if(customerOrderInt[idx][0] == makingOrderInt[0]){ // 성공
                        iceResult = 1
                        pay = calcCost(1, 1)

                    }else{ // 실패
                        iceResult = 0
                        pay = calcCost(1, 1, 1)
                    }
                }else if(customerOrderInt[idx][1] == -1 && makingOrderInt[1] != -1){ // 1단 주문, 2단 제작
                    iceResult = 0
                    pay = calcCost(2, 1)

                }else if(customerOrderInt[idx][1] != -1 && makingOrderInt[1] == -1){ // 2단 주문, 1단 제작
                    iceResult = 0
                    pay = calcCost(1, 1, 1)

                }else{ // 2단 주문, 2단 제작
                    if(customerOrderInt[idx][0] == makingOrderInt[0]){ // 1단 성공
                        if(customerOrderInt[idx][1] == makingOrderInt[1]){ // 2단 성공
                            iceResult = 1
                            pay = calcCost(2, 2)

                        }else{ // 2단 잘못 만듦
                            iceResult = 0
                            pay = calcCost(2, 2, 1)

                        }
                    }else{
                        if(customerOrderInt[idx][0] == makingOrderInt[1] && customerOrderInt[idx][1] == makingOrderInt[0]){
                            // 1단 2단 순서를 바꿔서만듦 -> 성공으로 간주
                            iceResult = 1
                            pay = calcCost(2, 2)
                        }else {
                            iceResult = 0
                            pay = calcCost(2, 2, 1)
                        }
                    }
                }

                salesVolume++ // 판매 개수 +1
                customerLayout[idx].setTag("taked") // 아이스크림 받음 상태로 셋팅
                customerPayTv[idx].setText(pay.toString())
            }

            binding.ivMakingCone.visibility = View.GONE
            binding.ivMakingIce1.visibility = View.GONE
            binding.ivMakingIce2.visibility = View.GONE
            makingBooleanList[0] = false
            makingBooleanList[1] = false
            makingBooleanList[2] = false
            customerOrderInt[idx][0] = -1
            customerOrderInt[idx][1] = -1
            makingOrderInt[0] = -1
            makingOrderInt[1] = -1

        }
    }

    fun calcCost(ingredient: Int, profit: Int, mistake: Int = 0): Int{

        var ingredientCost = 0
        var originalCost = 0
        var mistakeCost = 0

        if(ingredient == 1){ // 1단 아이스크림 재료값
            ingredientCost += (COST_ICE + COST_CONE)
        }else{ // 2단 아이스크림 재료값
            ingredientCost += (COST_ICE * 2 + COST_CONE)
        }

        if(profit == 1){ // 1단 아이스크림 판매 수익
            originalCost += COST_ICECREAM
        }else { // 2단 아이스크림
            originalCost += COST_ICECREAM2
        }

        if(mistake == 1){ // 실수 보상 비용
            mistakeCost += COST_MISMAKING
        }

        originalMoney += originalCost
        totalMoney += (originalCost-mistakeCost)
        mistakeMoney += mistakeCost
        ingredientMoney += ingredientCost

        return (originalCost-mistakeCost)
    }


    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        bgm.pause()
    }
    override fun onStop() {
        super.onStop()
        bgm.pause()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        bgm.stop()
    }

    inner class CustomerThread(idx: Int) : Thread() {
        val idx = idx

        override fun run() {
            while(customerThreadActive){
                try{
                    if(customerLayout[idx].getTag().toString() == "off"){ // idx 자리가 비었을 때

                        // customer drawable random
                        var character_random = Random().nextInt(6) // 캐릭터 6가지 중 1개 랜덤 선정

                        if(characterImageStatus[character_random][0] == "off"){ // 캐릭터 중복 제거
                            characterImageStatus[character_random][0] = "on"
                            characterImageStatus[character_random][1] = idx.toString()

                            handler.post {
                                customerCharacterIv[idx].setImageResource(customerSmileList[character_random])
                            }

                            var offTime = Random().nextInt(5000-1000)+1000 // 1 ~ 5초 중 랜덤
                            sleep(offTime.toLong())

                            if(gameOver == true) {
                                break
                            }

                            // 손님이 온 경우
                            customerLayout[idx].setTag("waiting") // idx 자리 채워짐

                            var comeMsg = Message()
                            comeMsg.arg1 = idx // 위치
                            comeHandler.handleMessage(comeMsg)

                            // 5초 기다림
                            for(i in 0 until 10){
                                sleep(500) // 0.5초 마다 아이스크림을 받았는지 확인
                                if(customerLayout[idx].getTag().toString() == "taked"){
                                    handler.post {
                                        if(iceResult == 0){
                                            customerCharacterIv[idx].setImageResource(customerSadList[character_random])
                                            customerResultIv[idx].setImageResource(R.drawable.no)
                                            customerResultIv[idx].visibility = View.VISIBLE
                                        }
                                        else if(iceResult == 1){
                                            customerCharacterIv[idx].setImageResource(customerLaughList[character_random])
                                            customerResultIv[idx].setImageResource(R.drawable.yes)
                                            customerResultIv[idx].visibility = View.VISIBLE
                                        }
                                        customerPayTv[idx].visibility = View.VISIBLE
                                        moneyTextView.setText(totalMoney.toString())
                                    }
                                    sleep(1000) // 1초 후 떠남
                                    break
                                }
                            }

                            if(customerLayout[idx].getTag().toString() == "waiting") { // 아직 기다리고 있는 중이라면
                                handler.post{
                                    customerCharacterIv[idx].setImageResource(customerSadList[character_random]) // 표정 변화
                                }

                                // 3초 더 기다림
                                for(i in 0 until 6){
                                    Thread.sleep(500) // 0.5초 마다 아이스크림을 받았는지 확인
                                    if(customerLayout[idx].getTag().toString() == "taked"){
                                        handler.post {
                                            if(iceResult == 0){
                                                customerCharacterIv[idx].setImageResource(customerSadList[character_random])
                                                customerResultIv[idx].setImageResource(R.drawable.no)
                                                customerResultIv[idx].visibility = View.VISIBLE
                                            }
                                            else if(iceResult == 1){
                                                customerCharacterIv[idx].setImageResource(customerLaughList[character_random])
                                                customerResultIv[idx].setImageResource(R.drawable.yes)
                                                customerResultIv[idx].visibility = View.VISIBLE
                                            }
                                            customerPayTv[idx].visibility = View.VISIBLE
                                            moneyTextView.setText(totalMoney.toString())
                                        }
                                        sleep(1000) // 1초 후 떠남
                                        break
                                    }
                                }

                                // 5초 더 기다렸는데도 아직 못 받았다면
                                if(customerLayout[idx].getTag().toString() == "waiting"){
                                    handler.post{
                                        customerCharacterIv[idx].setImageResource(customerAngryList[character_random]) // 표정 변화
                                        customerLayout[idx].setTag("angry")
                                    }
                                    sleep(500) // 0.5초 후 떠남
                                }
                            }


                            // 아이스크림을 받았거나 기다리다 지쳐서 떠남
                            var offMsg = Message()
                            offMsg.arg1 = idx
                            offMsg.arg2 = character_random
                            offHandler.handleMessage(offMsg)

                        }else{ continue }
                    }else{continue}
                } catch (e: InterruptedException){
                    e.printStackTrace()
                }
            }

        }

    }

    // 손님이 온 경우
    val comeHandler = object :Handler(){

        override fun handleMessage(msg: Message) {
            var idx = msg.arg1

            handler.post{

                // ice cream drawable random
                var num_random = Random().nextInt(2) // 아이스크림 덩어리 개수 랜덤
                var ice_random = Random().nextInt(5) // 아이스크림 맛 5가지 중 1개 랜덤 선정
                var ice_random2 = Random().nextInt(5)

                customerOrderInt[idx][0] = iceCreamList[ice_random]
                customerOrderIv[idx][0].setImageResource(customerOrderInt[idx][0])
                if(num_random == 0){ // 아이스크림 한 덩이
                    customerOrderIv[idx][1].visibility = View.GONE
                }else if(num_random == 1){ //  두 덩이
                    customerOrderInt[idx][1] = iceCreamList[ice_random2]
                    customerOrderIv[idx][1].visibility = View.VISIBLE
                    customerOrderIv[idx][1].setImageResource(customerOrderInt[idx][1])
                }
                customerLayout[idx].visibility = View.VISIBLE

            }

        }
    }

    // 손님이 떠나는 경우
    val offHandler = object: Handler(){

        override fun handleMessage(msg: Message) {
            var idx = msg.arg1
            //var face = msg.arg2 // 0이면 sad, 1이면 laugh
            var status = customerLayout[idx].getTag().toString()
            var imageIdx = -1

            // 손님 상태 초기화
            //customerImageStatus[msg.arg2][1] = "-1"
            //customerImageStatus[msg.arg2][0] = "off"
            handler.post {
                characterImageStatus[msg.arg2][1] = "-1"
                characterImageStatus[msg.arg2][0] = "off"
                customerLayout[idx].visibility = View.INVISIBLE
                customerResultIv[idx].visibility = View.GONE
                customerPayTv[idx].visibility = View.GONE
                customerLayout[idx].setTag("off")
            }
        }
    }
}



