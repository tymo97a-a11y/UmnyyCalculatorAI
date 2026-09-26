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
import kotlin.math.PI
import kotlin.math.sqrt

class GeometryActivity : AppCompatActivity() {
    private val bg = Color.rgb(7, 11, 20)
    private val card = Color.rgb(17, 26, 43)
    private val blue = Color.rgb(36, 107, 253)
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun params(h:Int=ViewGroup.LayoutParams.WRAP_CONTENT)=LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, if(h==ViewGroup.LayoutParams.WRAP_CONTENT) h else dp(h)).apply{topMargin=dp(10)}
    private fun input(hint:String)=EditText(this).apply{this.hint=hint;setTextColor(Color.WHITE);setHintTextColor(Color.rgb(145,160,184));textSize=16f;setSingleLine(true);setBackgroundColor(card);setPadding(dp(16),0,dp(16),0)}
    private fun label(s:String)=TextView(this).apply{text=s;setTextColor(Color.rgb(180,195,215));textSize=14f}
    private fun title(s:String)=TextView(this).apply{text=s;setTextColor(Color.WHITE);textSize=27f;setPadding(0,dp(8),0,dp(8))}
    private fun num(v:EditText)=v.text.toString().trim().replace(',','.').toDoubleOrNull()
    private fun out(s:String)=TextView(this).apply{text=s;setTextColor(Color.WHITE);textSize=16f;setPadding(dp(16),dp(16),dp(16),dp(16));setBackgroundColor(card)}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(20),dp(20),dp(30));setBackgroundColor(bg)}
        setContentView(ScrollView(this).apply{addView(root)})
        root.addView(Button(this).apply{text="← Назад";setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{finish()}},params(52))
        root.addView(title("📐 Геометрия"))
        root.addView(label("Площадь, периметр и теорема Пифагора."))
        root.addView(label("Прямоугольник: длина и ширина"))
        val rw=input("Например: 5");root.addView(rw,params(58))
        val rh=input("Например: 3");root.addView(rh,params(58))
        val rb=Button(this).apply{text="▣ Рассчитать прямоугольник";setTextColor(Color.WHITE);setBackgroundColor(blue)};root.addView(rb,params(58))
        val ro=out("Введите длину и ширину.");root.addView(ro,params())
        rb.setOnClickListener{val w=num(rw);val h=num(rh);ro.text=if(w!=null&&h!=null&&w>=0&&h>=0)"Площадь: ${fmt(w*h)}\nПериметр: @@{fmt(2*(w+h))}" else "Ошибка: введите положительные числа."}
        root.addView(label("Круг: радиус"))
        val rr=input("Например: 4");root.addView(rr,params(58))
        val rcb=Button(this).apply{text="◯ Рассчитать круг";setTextColor(Color.WHITE);setBackgroundColor(blue)};root.addView(rcb,params(58))
        val rco=out("Введите радиус.");root.addView(rco,params())
        rcb.setOnClickListener{val r=num(rr);rco.text=if(r!=null&&r>=0)"Площадь: @@{fmt(PI*r*r)}\nДлина окружности: @@{fmt(2*PI*r)}" else "Ошибка: радиус должен быть ≥ 0."}
        root.addView(label("Прямоугольный треугольник: катеты"))
        val ta=input("Катет a");root.addView(ta,params(58))
        val tb=input("Катет b");root.addView(tb,params(58))
        val tbtn=Button(this).apply{text="△ Рассчитать треугольник";setTextColor(Color.WHITE);setBackgroundColor(blue)};root.addView(tbtn,params(58))
        val to=out("Введите два катета.");root.addView(to,params())
        tbtn.setOnClickListener{val a=num(ta);val b=num(tb);to.text=if(a!=null&&b!=null&&a>0&&b>0)"Гипотенуза: @@{fmt(sqrt(a*a+b*b))}\nПлощадь: @@{fmt(a*b/2)}" else "Ошибка: катеты должны быть > 0."}
    }
    private fun fmt(v:Double)=if(v.isFinite())if(kotlin.math.abs(v-v.toLong())<1e-10)v.toLong().toString() else "%.4f".format(java.util.Locale.US,v).trimEnd('0').trimEnd('.') else "—"
}
