package com.veronikalebedyuk.dialogforbetter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.veronikalebedyuk.dialogforbetter.adapters.HintAdapter;
import com.veronikalebedyuk.dialogforbetter.adapters.MessageAdapter;
import com.veronikalebedyuk.dialogforbetter.adapters.RecyclerVIewClickInterface;
import com.veronikalebedyuk.dialogforbetter.classes.JavaMailWithAttachments;
import com.veronikalebedyuk.dialogforbetter.classes.MealPlan;
import com.veronikalebedyuk.dialogforbetter.classes.MedicalCritera;
import com.veronikalebedyuk.dialogforbetter.classes.Message;
import com.veronikalebedyuk.dialogforbetter.classes.Product;
import com.veronikalebedyuk.dialogforbetter.databases.DatabaseHelper;
import com.veronikalebedyuk.dialogforbetter.databases.FoodDatabaseHelper;
import com.veronikalebedyuk.dialogforbetter.databases.UnwantedFoodDatabaseHelper;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements RecyclerVIewClickInterface {
    private RecyclerView rv;
    private RecyclerView rv_hints;
    private EditText et;
    private ImageButton btn;
    private MessageAdapter adapter;
    private RecyclerView.Adapter hintAdapter;
    private List<Message> messages;
    private DatabaseHelper databaseHelper;
    private FoodDatabaseHelper foodDatabaseHelper;
    private UnwantedFoodDatabaseHelper unwantedFoodDatabaseHelper;
    private DatabaseReference myRef;
    private List <MedicalCritera> criterias;
    private String[] criteriaHints;
    private String[] insulineHints;
    private String[] BlankPhrases = new String[] {
            "Извините?", "Не понимаю :(", "Попробуйте ещё раз", "Я не знаю такой команды", "Команда не ясна" };
    private String[] timePeriodHints = new String[]{
            "неделю","месяц", "3 месяца","год"
    };
    private String[] hintsStringArray=new String[]{
            "показатель", "календарь", "расчет", "статистика", "план", "???"
    };
    private String[]chatHints = new String[]{
            "показатель", "календарь", "расчет", "статистика", "план","???"};
    private String[] calendarHints = new String[] {
            "добавить событие/удалить событие", "???" };
    private String[] yesnoHints = new String[] {
            "да", "нет" };
    private double PORTION_SIZE;
    private double SINGLE_XE_PORTION;
    private double EXTRA;
    private int WRONG_COMMAND_CNT;
    private int COMMAND_TYPE;
    private int ANSWER_STEP;
    private int INSULINE_TYPE;
    private int HEIGHT;
    private int WEIGHT;
    private int FIS_ACTIVITY_LEVEL;
    private int BODY_INDEX;
    private int DAY_NORM;
    private double CURRENT_INS_VALUE;
    private long STAT_PERIOD_BEGIN;
    private String EMAIL;
    private String NAME;
    private String KEY;
    private String STAT_CRITERIA;
    private SharedPreferences prefs;
    private MealPlan mealPlan;
    private Product PRODUCT;
    private DecimalFormat formater = new DecimalFormat("#.#");
    private LinearLayoutManager linearLayoutManager;
    private LinearLayoutManager linearLayoutManagerHints;

    private void checkFirstOpen(){
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        deleteDatabase("FoodDatabase");
        deleteDatabase("StatisticsDatabase");
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        rv_hints = findViewById(R.id.hints);
        rv = findViewById(R.id.message_list);
        btn = findViewById(R.id.btnSend);
        prefs = getSharedPreferences("prefs",MODE_PRIVATE);
        boolean firstOpen = prefs.getBoolean("first open",true);
        getDatabases();
        if(firstOpen) {
            checkFirstOpen();
            String[]foods = getResources().getStringArray(R.array.basic_foods);
            int[]food_values = getResources().getIntArray(R.array.basic_food_values);
            for(int i = 0; i<foods.length; i++){
                foodDatabaseHelper.addData(foods[i],food_values[i]);
            }
        }
        getPrefs();
        criterias = new ArrayList<>();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(KEY).child("MedCriterias");
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    MedicalCritera mc = ds.getValue(MedicalCritera.class);
                    criterias.add(mc);
                }
                criteriaHints = new String[criterias.size()];
                for(int i = 0; i<criterias.size();i++)criteriaHints[i]=criterias.get(i).getName();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        linearLayoutManagerHints = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rv_hints.setLayoutManager(linearLayoutManagerHints);
        hintAdapter = new HintAdapter(this,chatHints,this);
        hintAdapter.notifyDataSetChanged();
        rv_hints.setAdapter(hintAdapter);
        messages = new ArrayList<>();
        linearLayoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(linearLayoutManager);
        adapter = new MessageAdapter(this, messages);
        adapter.notifyDataSetChanged();
        myRef = FirebaseDatabase.getInstance().getReference("Users").child(KEY).child("Messages");
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                showData(dataSnapshot);
            }
            private void showData(DataSnapshot dataSnapshot) {
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    Message message = ds.getValue(Message.class);
                    messages.add(message);
                    Toast.makeText(MainActivity.this,ds.getKey(),Toast.LENGTH_LONG);
                }
                adapter = new MessageAdapter(MainActivity.this, messages);
                rv.setAdapter(adapter);
                linearLayoutManager.scrollToPosition(messages.size()-1);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                COMMAND_TYPE= prefs.getInt("command type",0);
                ANSWER_STEP= prefs.getInt("answer step",0);
                if(!et.getText().toString().trim().equals("")) {
                    addMessage( new Message(NAME,et.getText().toString(),"request"));
                    if (COMMAND_TYPE == 0) Answer(et.getText().toString());
                    else if (COMMAND_TYPE == 1) MedCriteriaScenario();
                    else if (COMMAND_TYPE == 2) CalculationScenario();
                    else if (COMMAND_TYPE == 3) CalendarScenario();
                    else if (COMMAND_TYPE == 4) StatisticScenario();
                    else if (COMMAND_TYPE == 5) MealPlanScenario();
                    et.setText("");
                }
                linearLayoutManager.scrollToPosition(messages.size()-1);
            }
        });
    }

    public void addMessage(Message m){
        messages.add(m);
        myRef.push().setValue(m);
    }
    public void addMessage(String DIAlogMessageText){
        Message m = new Message("DIAlog",DIAlogMessageText,"string answer");
        messages.add(m);
        myRef.push().setValue(m);
    }


    public void Answer(String mes) {
        if ("???".equals(mes) || WRONG_COMMAND_CNT == 3) {
            WRONG_COMMAND_CNT = 0;
            addMessage(getResources().getString(R.string.help_message));

        }
        else if ("показатель".equals(mes)) {
            WRONG_COMMAND_CNT = 0;
            addMessage("по какому критерию Вы собираетесь занести показания?");
            changeCommandType(1);
            changeAnswerStep(0);
            changeHints(criteriaHints);
        }
        else if ("расчет".equalsIgnoreCase(mes)) {
            WRONG_COMMAND_CNT = 0;
            changeCommandType(2);
            changeAnswerStep(0);
            CalculationScenario();
        }
        else if ("календарь".equalsIgnoreCase(mes)) {
            WRONG_COMMAND_CNT = 0;
            addMessage(
                    new Message("DIAlog","current date","calendar answer"));

            addMessage("сверьтесь с календарем выше");
            addMessage("хотите добавить новое событие?");
            changeCommandType(3);
            changeAnswerStep(0);
            changeHints(yesnoHints);
        }
        else if ("статистика".equalsIgnoreCase(mes)) {
            WRONG_COMMAND_CNT = 0;
            databaseHelper = new DatabaseHelper(MainActivity.this);
            changeCommandType(4);
            addMessage("статистику по какому критерию вы хотите получить?");
            changeHints(criteriaHints);
            changeAnswerStep(0);
        }
        else if ("план".equalsIgnoreCase(mes)) {
            WRONG_COMMAND_CNT = 0;
            changeCommandType(5);
            changeAnswerStep(prefs.getInt("plan step",-4));
            MealPlanScenario();
        }
        else{
            WRONG_COMMAND_CNT ++;
            Random random = new Random();
            addMessage(BlankPhrases[random.nextInt(BlankPhrases.length)]);
        }
    }

    int MED_CRITERIA_TYPE;
    public void MedCriteriaScenario(){
        String lastUserMessage = messages.get(messages.size()-1).messageText;
        if(ANSWER_STEP == 0){
            for(int i = 0; i < criterias.size(); i++){
                if(criterias.get(i).getName().equals(lastUserMessage)) {
                    MED_CRITERIA_TYPE = i;
                    addMessage(criterias.get(MED_CRITERIA_TYPE).InputRequest());
                    changeAnswerStep(1);
                    et.setInputType(InputType.TYPE_CLASS_NUMBER);
                    break;
                }
                else if(i == criterias.size()-1){
                    changeCommandType(0);
                    changeHints();
                }
            }
        }
        else if(ANSWER_STEP == 1){
            et.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
            try {
                double value = Double.parseDouble(lastUserMessage);
                databaseHelper = new DatabaseHelper(MainActivity.this);
                databaseHelper.addData(value, criterias.get(MED_CRITERIA_TYPE).getName());
                String s = criterias.get(MED_CRITERIA_TYPE).Estimate(value);
                addMessage(s);
                if(criterias.get(MED_CRITERIA_TYPE).getName().equals("сахар"))CURRENT_INS_VALUE = value;
                if(criterias.get(MED_CRITERIA_TYPE).getName().equals("вес")){
                    double changeProportion = value/WEIGHT;
                    if(prefs.getInt("plan step",-4)==0){
                        prefs.edit().putInt("body index",(int)(BODY_INDEX*changeProportion)).apply();
                        prefs.edit().putInt("day norm",(int)(DAY_NORM*changeProportion)).apply();
                        addMessage("план питания мог быть изменен");
                        changeCommandType(0);
                        changeHints();
                    }
                    WEIGHT = (int)value;
                    prefs.edit().putInt("weight",(int)value).apply();
                }
                if(s.equalsIgnoreCase("показатель значительно ниже нормы, хотите назначить визит к врачу?")||
                        s.equalsIgnoreCase("показатель значительно привышает норму, хотите назначить визит к врачу?")){
                    changeHints(yesnoHints);
                    changeAnswerStep(2);
                }
                else if(criterias.get(MED_CRITERIA_TYPE).getName().equals("сахар") && CURRENT_INS_VALUE>7){
                    addMessage("хотите расчитать необходимую дозу инсулина?");
                    changeHints(yesnoHints);
                    changeAnswerStep(3);
                }
                else{
                    changeCommandType(0);
                    changeHints();
                }

            } catch (NumberFormatException | NullPointerException nfe) {
                changeCommandType(0);
                changeHints();
                Answer(lastUserMessage);
            }

        }
        else if(ANSWER_STEP==2){
            if(lastUserMessage.equalsIgnoreCase("да")){
                changeHints();
                Intent i = new Intent(this, CalendarActivity.class);
                startActivity(i);
            }
            else if(criterias.get(MED_CRITERIA_TYPE).getName().equals("сахар") && CURRENT_INS_VALUE>3){
                addMessage("хотите расчитать необходимую дозу инсулина?");
                changeHints(yesnoHints);
                changeAnswerStep(3);
            }
            else if(criterias.get(MED_CRITERIA_TYPE).getName().equals("сахар") && CURRENT_INS_VALUE<3){
                addMessage("срочно примите что-нибудь содержащее быстрые углеводы");
                changeCommandType(0);
                changeHints();
            }
            else {
                changeCommandType(0);
                changeHints();
            }
        }
        else if(ANSWER_STEP==3){
            if(lastUserMessage.trim().equalsIgnoreCase("да")){
                changeHints();
                changeAnswerStep(5);
                changeCommandType(2);
                CalculationScenario();
            }
            else{
                changeCommandType(0);
                changeHints();
            }
        }
    }
    public void CalculationScenario(){
        String lastUserMessage = messages.get(messages.size()-1).messageText;
        foodDatabaseHelper = new FoodDatabaseHelper(MainActivity.this);
        Cursor cursor = foodDatabaseHelper.getData();
        EditText et = findViewById(R.id.text_input);
         if(ANSWER_STEP == 0){
            addMessage("введите название съеденной пищи");
            changeAnswerStep(1);
        }
        else if(ANSWER_STEP == 1){
            changeHints();
            PRODUCT = new Product();
            PRODUCT.setName(lastUserMessage);
            List<String>foodOptions = new ArrayList<>();
            int cnt = 0;
            while(cursor.moveToNext()){
                if(cursor.getString(1).equalsIgnoreCase(lastUserMessage)){
                    SINGLE_XE_PORTION = cursor.getInt(2);
                    cnt=1;
                    break;
                }
                else if(cursor.getString(1).toLowerCase().contains(lastUserMessage.toLowerCase())){
                    foodOptions.add(cursor.getString(1));
                    cnt++;
                    if(cnt==0)SINGLE_XE_PORTION = cursor.getInt(2);
                }
            }
            if(cnt>1){
                String[] fo = new String[foodOptions.size()];
                foodOptions.toArray(fo);
                changeHints(fo);
                addMessage("выберите вариант");
                changeAnswerStep(1);
            }
            else if (cnt==0){
                et.setInputType(InputType.TYPE_CLASS_NUMBER);
                addMessage("к сожалению пищевая ценность порции пока не известна,\n\nвведите его массу в граммах на 1 ХЕ");
                changeAnswerStep(7);
            }
            else{
                changeAnswerStep(2);
                CalculationScenario();
            }
        }
         else if(ANSWER_STEP == 2){
             et.setInputType(InputType.TYPE_CLASS_NUMBER);
             addMessage("введите размер порции в граммах");
             changeAnswerStep(3);
         }

        else if(ANSWER_STEP == 3){
             try {
                 PORTION_SIZE = Double.parseDouble(lastUserMessage);
                 EXTRA = 2*PORTION_SIZE/SINGLE_XE_PORTION;
                 addMessage("введите Ваш текущий уровень гликемии");
                 changeAnswerStep(4);
             } catch (NumberFormatException | NullPointerException nfe) {
                 changeCommandType(0);
                 changeHints();
                 Answer(lastUserMessage);
             }
        }
        else if(ANSWER_STEP == 4){
             try {
                 CURRENT_INS_VALUE = Double.parseDouble(lastUserMessage);
                 databaseHelper.addData(CURRENT_INS_VALUE,"сахар");
                 changeAnswerStep(5);
                 CalculationScenario();

             } catch (NumberFormatException | NullPointerException nfe) {
                 changeCommandType(0);
                 changeHints();
                 Answer(lastUserMessage);
             }

         }
        else if(ANSWER_STEP == 5){
             et.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
             if (CURRENT_INS_VALUE<5){
                 addMessage("уровень сахара в крови слишком низок, чтобы корректировать его инсулином");
                 changeCommandType(0);
                 changeAnswerStep(0);
             }
             else{
                 CURRENT_INS_VALUE+=EXTRA;
                 INSULINE_TYPE = getSharedPreferences("prefs",MODE_PRIVATE).getInt("insulineType", 2);
                 if(INSULINE_TYPE==2){
                    addMessage("выберите тип инсулина");
                    changeAnswerStep(-1);
                    changeHints(new String[]{"короткий","ультракороткий"});
                 }
                 else{
                     changeAnswerStep(5);
                     CalculationScenario();
                 }
             }
        }
        else if(ANSWER_STEP== -1){
            if(lastUserMessage.trim().equalsIgnoreCase("короткий"))INSULINE_TYPE=0;
            else if (lastUserMessage.trim().equalsIgnoreCase("ультракороткий"))INSULINE_TYPE = 1;
            changeAnswerStep(6);
            CalculationScenario();
        }
        else if(ANSWER_STEP == 6){
             double k;
             int sdi = getSharedPreferences("prefs",MODE_PRIVATE).getInt("SDI",50);
             if(INSULINE_TYPE==0) k = 83/sdi;
             else k = 100/sdi;
             double dose = (Math.abs(CURRENT_INS_VALUE - 7)) / k;
             String ans = "Расчитанная доза - " + formater.format(dose) + " Ед";
             addMessage(ans);
             CURRENT_INS_VALUE = 0;
             changeCommandType(0);
             changeHints();

        }
        else if(ANSWER_STEP == 7){
            double eValue;
            try {
                eValue = Double.parseDouble(lastUserMessage.trim());
                PRODUCT.setValue(eValue);
                foodDatabaseHelper = new FoodDatabaseHelper(MainActivity.this);
                foodDatabaseHelper.addData(PRODUCT.getName(),eValue);
                SINGLE_XE_PORTION = eValue;
                changeAnswerStep(3);
                CalculationScenario();
            } catch (NumberFormatException | NullPointerException nfe) {
                changeCommandType(0);
                changeHints();
                et.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                Answer(lastUserMessage);
            }
        }
    }

    public void CalendarScenario(){
        String lastUserMessage = messages.get(messages.size()-1).messageText;
        if(ANSWER_STEP == 0){
            if(lastUserMessage.trim().equalsIgnoreCase("да")|| lastUserMessage.equals("добавить событие/удалить событие")){
                Intent i = new Intent(this, CalendarActivity.class);
                startActivity(i);
            }
            else{
                changeCommandType(0);
                changeHints();
            }
        }
    }

    public void StatisticScenario(){
        String lastUserMessage = messages.get(messages.size()-1).messageText;
        if(ANSWER_STEP == 0){
            STAT_CRITERIA = lastUserMessage;
            changeAnswerStep(1);
            addMessage("вывести статистику за прошедшие...");
            changeHints(timePeriodHints);
        }
        else if(ANSWER_STEP == 1){
            long currentTIme = System.currentTimeMillis();
            long oneWeek =1000*60*60*24*7;
            long oneMonth =1000*60*60*24*30;
            long threeMonths =1000*60*60*24*30*3;
            long oneYear =1000*60*60*24*365;
           if(lastUserMessage.equalsIgnoreCase(chatHints[0])){
               addMessage(
                       new Message("DIAlog",STAT_CRITERIA,"graph0"));
               STAT_PERIOD_BEGIN = currentTIme -oneWeek;
           }
            if(lastUserMessage.equalsIgnoreCase(chatHints[1])){
                addMessage(
                        new Message("DIAlog",STAT_CRITERIA,"graph1"));
                STAT_PERIOD_BEGIN = currentTIme -oneMonth;
            }
            if(lastUserMessage.equalsIgnoreCase(chatHints[2])){
                addMessage(
                        new Message("DIAlog",STAT_CRITERIA,"graph2"));
                STAT_PERIOD_BEGIN = currentTIme-threeMonths;
            }
            else{
                addMessage(
                        new Message("DIAlog",STAT_CRITERIA,"graph3"));
                STAT_PERIOD_BEGIN = currentTIme -oneYear;
            }
            addMessage("вот визуализация, вводимых вами данных. Файл в формате excel с чиловыми данными был выслан на электронную почту");
            sendStatisticEmail();
            changeCommandType(0);
            changeHints();

        }
    }
    public void MealPlanScenario(){
        double x;
        String lastUserMessage = messages.get(messages.size()-1).messageText;
        if(ANSWER_STEP==-4){
            mealPlan = new MealPlan();
            x = WEIGHT/(HEIGHT*HEIGHT * 0.0001);
            Toast.makeText(MainActivity.this,"h: "+ HEIGHT + " w: "+ WEIGHT + " i:" + x,Toast.LENGTH_LONG).show();
            mealPlan.setBodyMassIndex((int)x);
            prefs.edit().putInt("body index",(int)x).apply();
            BODY_INDEX = mealPlan.getBodyMassIndex();
            if(BODY_INDEX>30)mealPlan.setDayNorm(1300);
            prefs.edit().putFloat("activity level", BODY_INDEX);
            addMessage("выберите уровень своей физической активности");
            changeHints(
                    new String[]{
                            "низкий","средний","высокий"
                    }
            );
            changeAnswerStep(-3);
        }
        else if(ANSWER_STEP==-3){
            if(lastUserMessage.equalsIgnoreCase("низкий")){
                prefs.edit().putInt("activity level", 11).apply();
                mealPlan.setFisActivityLevel(11);
            }
            if(lastUserMessage.equalsIgnoreCase("средний")){
                prefs.edit().putInt("activity level", 13).apply();
                mealPlan.setFisActivityLevel(13);
            }
            if(lastUserMessage.equalsIgnoreCase("высокий")){
                prefs.edit().putInt("activity level", 15).apply();
                mealPlan.setFisActivityLevel(15);
            }
            x = mealPlan.getKkPerKg()* WEIGHT * mealPlan.getFisActivityLevel()*0.1;
            if(mealPlan.getDayNorm()!=1300)mealPlan.setDayNorm((int) x);
            prefs.edit().putInt("day norm", mealPlan.getDayNorm()).apply();
            addMessage("есть ли продукты, которые бы Вы не хотели видеть в соем плане питания?");
            changeHints(yesnoHints);
            changeAnswerStep(-2);
        }
        else if(ANSWER_STEP==-2){
            if (lastUserMessage.equalsIgnoreCase("да")){
                addMessage("введите названия одного из нежелательных продуктов");
                changeAnswerStep(-1);
            }
            else{
                mealPlan = new MealPlan();
                changeAnswerStep(0);
                MealPlanScenario();
            }
        }
        else if(ANSWER_STEP==-5){
            addMessage("есть ли ЕЩЁ продукты, которые бы Вы не хотели видеть в соем плане питания?");
            changeHints(yesnoHints);
            changeAnswerStep(-1);
        }
        else if(ANSWER_STEP==-1){
            List<String>foodOptions = new ArrayList<>();
            int cnt = 0;
            Cursor cursor = foodDatabaseHelper.getData();
            String s="";
            while(cursor.moveToNext()){
                if(cursor.getString(1).toLowerCase().contains(lastUserMessage.toLowerCase())){
                    s= cursor.getString(1);
                    foodOptions.add(cursor.getString(1));
                    cnt++;
                }
            }
            Integer d;
            if(cnt>1){
                Message m = new Message("DIAlog: уточните свой ответ, выбрав один или несколько вариантов, а затем ответьте хотите ли вы исключть каки-либо еще наименования", lastUserMessage, "choice");
                addMessage(m);
                changeAnswerStep(-2);
            }
            else if(cnt==1) {
                d = foodDatabaseHelper.deleteData(s);
                unwantedFoodDatabaseHelper.addData(s,-1);
                changeAnswerStep(-5);
                MealPlanScenario();
            }
            else{
                unwantedFoodDatabaseHelper.addData(s,-1);
                changeAnswerStep(0);
                MealPlanScenario();
            }
        }
        else if(ANSWER_STEP==0){
            prefs.edit().putInt("plan step",0).apply();
            FIS_ACTIVITY_LEVEL = prefs.getInt("activity level", 11);
            BODY_INDEX = prefs.getInt("body index", 25);
            DAY_NORM = prefs.getInt("day norm", 1300);
            Toast.makeText(MainActivity.this, "day norm "+ DAY_NORM,Toast.LENGTH_LONG).show();
            mealPlan = new MealPlan(FIS_ACTIVITY_LEVEL, BODY_INDEX, DAY_NORM);
            changeAnswerStep(1);
            addMessage(mealPlan.createMealPlan());
            addMessage("предложить вариант приема пищи?");
            changeHints(yesnoHints);
        }
        else if(ANSWER_STEP==1){
            if(lastUserMessage.equalsIgnoreCase("да")) {
                addMessage("судя по часам, сейчас время " + mealPlan.mealType()
                        + "\nмогу предложить сьесть " + foodSuggestion());
                changeAnswerStep(2);
            }
            changeCommandType(0);
                changeHints();
        }
    }

    public void sendStatisticEmail(){
        Workbook workbook = new HSSFWorkbook();
        Cell cell =null;
        CreationHelper createHelper = workbook.getCreationHelper();
        CellStyle cellStyle = workbook.createCellStyle();
        CellStyle dateCellStyle = workbook.createCellStyle();
        dateCellStyle.setDataFormat(
                createHelper.createDataFormat().getFormat("yyyy.MM.dd"));
        CellStyle timeCellStyle = workbook.createCellStyle();
        timeCellStyle.setDataFormat(
                createHelper.createDataFormat().getFormat("HH:mm:ss"));

        Sheet sheet = null;
        sheet = workbook.createSheet(STAT_CRITERIA);
        Row row =sheet.createRow(0);
        cell= row.createCell(0);
        cell.setCellValue("дата");
        cell.setCellStyle(cellStyle);
        cell= row.createCell(1);
        cell.setCellValue("время");
        cell.setCellStyle(cellStyle);
        cell= row.createCell(2);
        cell.setCellValue("значение");
        cell.setCellStyle(cellStyle);
        int cnt = 1;

        databaseHelper = new DatabaseHelper(MainActivity.this);
        Cursor cursor = databaseHelper.getData();

        while(cursor.moveToNext()){
            if(
                    (cursor.getString(3).equalsIgnoreCase(STAT_CRITERIA) && STAT_PERIOD_BEGIN<=cursor.getLong(1))
            ) {
                Date date = new Date(cursor.getLong(1));
                row = sheet.createRow(cnt);
                cell = row.createCell(0);
                cell.setCellValue((String) android.text.format.DateFormat.format("yyyy.MM.dd", date));
                cell.setCellStyle(dateCellStyle);
                cell = row.createCell(1);
                cell.setCellValue((String) android.text.format.DateFormat.format("HH:mm:ss", date));
                cell.setCellStyle(timeCellStyle);
                cell = row.createCell(2);
                cell.setCellValue(cursor.getInt(2));
                cell.setCellStyle(cellStyle);
                cnt++;
            }

        }

        String filename ="statistics" + (String) android.text.format.DateFormat.format("yyyyMMddHHmmss",new Date(System.currentTimeMillis())) + ".xls" ;
        FileOutputStream outputStream = null;
        try {
            outputStream =openFileOutput(filename,MODE_PRIVATE);
            workbook.write(outputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = new File(getFilesDir() +"/"+filename);
        JavaMailWithAttachments javaMailWithAttachments = new JavaMailWithAttachments(this,EMAIL,"DIAlog for BETter статистика","", file);
        javaMailWithAttachments.execute();
    }

    public void changeParameter(){
        if(ANSWER_STEP==0){

        }
    }

    public String foodSuggestion(){
        Date time = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("HH");
        Resources res = getResources();
        String[]foodOptions;
        Random random = new Random();
        int h =Integer.parseInt(sdf.format(time));
        if(h<12 && h>5){
            foodOptions = res.getStringArray(R.array.Breakfast);
        }
        else if(h<15){
            foodOptions = res.getStringArray(R.array.LunchFirst);
        }
        else if(h<19){
            foodOptions = res.getStringArray(R.array.Dinner);
        }
        else{
            foodOptions = res.getStringArray(R.array.Snacks);
        }
        return foodOptions[random.nextInt(foodOptions.length)];
    }
    public void changeHints(String[] hints){
        rv_hints = (RecyclerView) findViewById(R.id.hints);
        LinearLayoutManager linearLayoutManagerHints = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rv_hints.setLayoutManager(linearLayoutManagerHints);
        hintAdapter = new HintAdapter(this,hints,this);
        rv_hints.setAdapter(hintAdapter);
        hintsStringArray = hints;
    }

    public void changeHints(){
        rv_hints = (RecyclerView) findViewById(R.id.hints);
        LinearLayoutManager linearLayoutManagerHints = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rv_hints.setLayoutManager(linearLayoutManagerHints);
        if(COMMAND_TYPE == 1) {
            hintAdapter = new HintAdapter(this,criteriaHints,this);
            hintsStringArray = criteriaHints;
        }
        else if (COMMAND_TYPE == 3){
            hintAdapter = new HintAdapter(this,calendarHints,this);
            hintsStringArray = calendarHints;
        }
        else {
            hintAdapter = new HintAdapter(this,chatHints,this);
            hintsStringArray = chatHints;
        }
        rv_hints.setAdapter(hintAdapter);
    }

    public void getPrefs(){
        HEIGHT = prefs.getInt("height",170);
        WEIGHT = prefs.getInt("weight",60);
        EMAIL = prefs.getString("email","example@gmail.com");
        NAME = prefs.getString("name","USERNAME");
        FIS_ACTIVITY_LEVEL = prefs.getInt("activity level",13);
        BODY_INDEX = prefs.getInt("body index",25);
        DAY_NORM = prefs.getInt("day norm",1300);
        KEY = getSharedPreferences("prefs",MODE_PRIVATE).getString("user", "TestUser");
        INSULINE_TYPE = getSharedPreferences("prefs",MODE_PRIVATE).getInt("insulineType", 2);
        if(INSULINE_TYPE==0)insulineHints=new String[]{"короткий" , "ультракороткий"};
    }
    public void getDatabases(){
        foodDatabaseHelper = new FoodDatabaseHelper(this);
        unwantedFoodDatabaseHelper = new UnwantedFoodDatabaseHelper(this);
        databaseHelper = new DatabaseHelper(this);
    }

    public void changeAnswerStep(int to){
        ANSWER_STEP=to;
        prefs.edit().putInt("answer step",to).apply();
    }
    public void changeCommandType(int to){
        COMMAND_TYPE=to;
        prefs.edit().putInt("command type",to).apply();
    }

    @Override
    public void onItemClick(int position) {
        et = (EditText) findViewById(R.id.text_input);
        et.setText(hintsStringArray[position]);
    }
}