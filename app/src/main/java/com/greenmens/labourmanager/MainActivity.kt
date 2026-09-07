package com.greenmens.labourmanager

import android.app.*
import android.os.Bundle
import android.content.*
import android.graphics.Bitmap
import android.provider.MediaStore
import android.view.View
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private lateinit var db: LabourDb
    private lateinit var total: TextView; private lateinit var present: TextView; private lateinit var wages: TextView
    override fun onCreate(b: Bundle?) { super.onCreate(b); setContentView(R.layout.activity_main); db=LabourDb(this)
        total=findViewById(R.id.totalText); present=findViewById(R.id.presentText); wages=findViewById(R.id.wageText)
        findViewById<TextView>(R.id.dateText).text=SimpleDateFormat("EEEE, dd MMMM yyyy",Locale.getDefault()).format(Date())
        findViewById<Button>(R.id.addBtn).setOnClickListener{addLabour()}
        findViewById<Button>(R.id.listBtn).setOnClickListener{showLabourList()}
        findViewById<Button>(R.id.attendanceBtn).setOnClickListener{faceAttendance()}
        findViewById<Button>(R.id.paymentBtn).setOnClickListener{paymentDialog()}
        findViewById<Button>(R.id.reportBtn).setOnClickListener{reportDialog()}
        refresh()
    }
    private fun refresh(){ total.text="Total Labour\n${db.countLabour()}"; present.text="Present Today\n${db.presentToday()}"; wages.text="This Month Wages\n₹${db.monthWages()}" }
    private fun addLabour(){
        val box=LinearLayout(this); box.orientation=LinearLayout.VERTICAL; box.setPadding(30,10,30,0)
        val n=EditText(this); n.hint="Full name"; val m=EditText(this); m.hint="Mobile"; val r=EditText(this); r.hint="Wage ₹/day"; r.inputType=2; val v=EditText(this); v.hint="Village"
        box.addView(n);box.addView(m);box.addView(r);box.addView(v)
        AlertDialog.Builder(this).setTitle("Add New Labour").setView(box).setPositiveButton("Save"){_,_-> if(n.text.isNotBlank()){db.addLabour(n.text.toString(),m.text.toString(),r.text.toString().toDoubleOrNull()?:0.0,v.text.toString());refresh()}}.setNegativeButton("Cancel",null).show()
    }
    private fun showLabourList(){ val names=db.allLabour(); if(names.isEmpty()){Toast.makeText(this,"No labour added yet",Toast.LENGTH_SHORT).show();return}; AlertDialog.Builder(this).setTitle("Labour List").setItems(names.toTypedArray(),null).setPositiveButton("Close",null).show() }
    private fun faceAttendance(){
        // Camera capture is intentionally kept simple in v1. A production build should add an on-device face-recognition model and encrypted face templates.
        startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        AlertDialog.Builder(this).setTitle("Face Attendance").setMessage("Camera opened. After capture, choose the worker to mark Present.\n\nNext upgrade: automatic face matching with an on-device recognition model.").setPositiveButton("Choose Worker"){_,_->markWorker()}.setNegativeButton("Cancel",null).show()
    }
    private fun markWorker(){ val names=db.allLabour(); if(names.isEmpty()){Toast.makeText(this,"Add labour first",Toast.LENGTH_SHORT).show();return}; AlertDialog.Builder(this).setTitle("Mark Present").setItems(names.toTypedArray()){_,which-> db.attend(names[which],1.0);refresh();Toast.makeText(this,"Attendance saved",Toast.LENGTH_SHORT).show()}.show() }
    private fun paymentDialog(){ val e=EditText(this); e.hint="Amount paid ₹"; e.inputType=2; AlertDialog.Builder(this).setTitle("Record Payment").setView(e).setPositiveButton("Save"){_,_->Toast.makeText(this,"Payment entry saved for the selected worker in the next full module",Toast.LENGTH_LONG).show()}.setNegativeButton("Cancel",null).show() }
    private fun reportDialog(){ AlertDialog.Builder(this).setTitle("Reports").setMessage("Labour: ${db.countLabour()}\nPresent today: ${db.presentToday()}\nThis month wages: ₹${db.monthWages()}\n\nFull PDF/Excel export will be enabled in the next build.").setPositiveButton("OK",null).show() }
}

class LabourDb(ctx: Context){
    private val p=ctx.getSharedPreferences("labour_db",Context.MODE_PRIVATE)
    private fun key(k:String)=p.getStringSet(k,emptySet())!!.toMutableSet()
    fun addLabour(n:String,m:String,r:Double,v:String){ val id=System.currentTimeMillis().toString(); val s=key("labours"); s.add(id);p.edit().putStringSet("labours",s).putString("n_$id",n).putString("m_$id",m).putFloat("r_$id",r.toFloat()).putString("v_$id",v).apply() }
    fun countLabour()=key("labours").size
    fun allLabour():List<String>{return key("labours").map{p.getString("n_$it","")!!}.sorted()}
    fun attend(name:String,day:Double){ val d=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date());p.edit().putBoolean("a_${d}_$name",true).apply() }
    fun presentToday():Int{val d=SimpleDateFormat("yyyy-MM-dd",Locale.US).format(Date());return allLabour().count{p.getBoolean("a_${d}_$it",false)}}
    fun monthWages():Long{val cal=Calendar.getInstance();val ym=SimpleDateFormat("yyyy-MM",Locale.US).format(cal.time);var sum=0.0;for(id in key("labours")){val n=p.getString("n_$id","")!!;val r=p.getFloat("r_$id",0f).toDouble();for(day in 1..31){val k="a_${ym}-${String.format("%02d",day)}_$n";if(p.getBoolean(k,false))sum+=r}};return sum.toLong()}
}
