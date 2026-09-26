package com.smartcalculator.ai

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow

class FinanceActivity : AppCompatActivity() {
    private val bg=Color.rgb(7,11,20);private val card=Color.rgb(17,26,43);private val blue=Color.rgb(36,107,253)
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun p(h:Int=ViewGroup.LayoutParams.WRAP_CONTENT)=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,if(h==ViewGroup.LayoutParams.WRAP_CONTENT)h else dp(h)).apply{topMargin=dp(10)}
    private fun input(h:String)=EditText(this).apply{hint=h;setTextColor(Color.WHITE);setHintTextColor(Color.rgb(145,160,184));textSize=16f;setSingleLine(true);setBackgroundColor(card);setPadding(dp(16),0,dp(16),0)}
    private fun n(e:EditText)=e.text.toString().trim().replace(',','.').toDoubleOrNull()
    private fun out(s:String)=TextView(this).apply{text=s;setTextColor(Color.WHITE);textSize=16f;setPadding(dp(16),dp(16),dp(16),dp(16));setBackgroundColor(card)}
    override fun onCreate(b:Bundle?){super.onCreate(b);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(20),dp(20),dp(30));setBackgroundColor(bg)};setContentView(ScrollView(this).apply{addView(root)})
        root.addView(Button(this).apply{text="← Назад";setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{finish()}},p(52))
        root.addView(TextView(this).apply{text="💰 Финансовые расчёты";setTextColor(Color.WHITE);textSize=27f})
        root.addView(TextView(this).apply{text="Проценты, скидка, простой и сложный процент.";setTextColor(Color.rgb(180,195,215));textSize=14f})
        root.addView(TextView(this).apply{text="Скидка";setTextColor(Color.WHITE);textSize=19f})
        val price=input("Цена");root.addView(price,p(58));val disc=input("Скидка, %");root.addView(disc,p(58));val db=Button(this).apply{text="Рассчитать скидку";setTextColor(Color.WHITE);setBackgroundColor(blue)};root.addView(db,p(58));val dout=out("Введите цену и процент.");root.addView(dout,p())
        db.setOnClickListener{val a=n(price);val d=n(disc);dout.text=if(a!=null&&d!=null&&a>=0&&d>=0)"Сумма скидки: ${fmt(a*d/100)}\nЦена после скидки: @@{fmt(a*(1-d/100))}" else "Ошибка ввода."}
        root.addView(TextView(this).apply{text="Простой процент";setTextColor(Color.WHITE);textSize=19f})
        val principal=input("Сумма");root.addView(principal,p(58));val rate=input("Ставка, % в год");root.addView(rate,p(58));val years=input("Срок, лет");root.addView(years,p(58));val sb=Button(this).apply{text="Рассчитать";setTextColor(Color.WHITE);setBackgroundColor(blue)};root.addView(sb,p(58));val so=out("Введите сумму, ставку и срок.");root.addView(so,p())
        sb.setOnClickListener{val a=n(principal);val r=n(rate);val y=n(years);so.text=if(a!=null&&r!=null&&y!=null&&a>=0&&r>=0&&y>=0)"Доход: @@{fmt(a*r*y/100)}\nИтоговая сумма: @@{fmt(a*(1+r*y/100))}" else "Ошибка ввода."}
        root.addView(TextView(this).apply{text="Сложный процент";setTextColor(Color.WHITE);textSize=19f})
        val cp=input("Начальная сумма");root.addView(cp,p(58));val cr=input("Ставка, % за период");root.addView(cr,p(58));val periods=input("Количество периодов");root.addView(periods,p(58));val cb=Button(this).apply{text="Рассчитать";setTextColor(Color.WHITE);setBackgroundColor(blue)};root.addView(cb,p(58));val co=out("Введите параметры.");root.addView(co,p())
        cb.setOnClickListener{val a=n(cp);val r=n(cr);val k=n(periods);co.text=if(a!=null&&r!=null&&k!=null&&a>=0&&r>=-100&&k>=0)"Итоговая сумма: @@{fmt(a*(1+r/100).pow(k))}" else "Ошибка ввода."}
    }
    private fun fmt(v:Double)=if(v.isFinite())"%.2f".format(java.util.Locale.US,v) else "—"
}
