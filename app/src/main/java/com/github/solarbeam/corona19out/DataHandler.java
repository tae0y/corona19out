package com.github.solarbeam.corona19out;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class DataHandler {
    static private HashMap<String, District> districtList = new HashMap<>();
    static private ArrayList<NotIdentifiedContact> contactList = new ArrayList<>();

    public static void _testBusan(Context ctx) throws IOException {
        //GET Patient Number
        getConfirmedPatientNumberList("부산", "https://www.busan.go.kr/covid19/Course01.do");

        //GET Patient Contact Info
        getNotIdentifiedContact("부산", "https://www.busan.go.kr/covid19/Corona19/travelhist.do");

        //DISPLAY Patient Number, https://codechacha.com/ko/java-sort-map/
        List<String> keyList = new ArrayList<>(districtList.keySet());
        keyList.sort(new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareTo(s2);
            }
        });
        ArrayList<String> strlist = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for(String key : keyList){
            String date; Calendar now = districtList.get(key).lastlyConfirmedDate;
            if(now==null) date = "-"; else date = (now.get(Calendar.MONTH)+1) + "월" + (now.get(Calendar.DATE)) + "일";
            strlist.add(districtList.get(key).name + " (" + date + ") : " + districtList.get(key).lastlyConfirmedPatientNumber);
            sb.append(districtList.get(key).name + " (" + date + ") : " + districtList.get(key).lastlyConfirmedPatientNumber + "\n");
        }
        //System.out.println(sb.toString());
        Log.d("PARSEWEB", sb.toString());

        //DISPLAY Patient Contact Info
        StringBuilder addressStr = new StringBuilder(), coord = new StringBuilder();
        Geocoder gc = new Geocoder(ctx, Locale.KOREA);
        for(NotIdentifiedContact c : contactList){
            //System.out.println(c.address);
            addressStr.append(c.address+"\n");
            List<Address> list = gc.getFromLocationName(c.address, 10);
            coord.append(list.get(0).getLatitude()+", "+list.get(0).getLongitude()+"\n");
        }
        Log.d("PARSEWEB", addressStr.toString());
        Log.d("PARSEWEB", coord.toString());

    }



    public static void getConfirmedPatientNumberList(String cityName, String url) throws IOException {
        if(cityName.equals("부산")){
            Elements docElementList = Jsoup.connect(url)
                    .get()
                    .select("div[class=corona_list]")
                    .select("ul[class$=active]");
            for(Element patient : docElementList){
                //확진자번호, 확진일자, 지역, 감염경로, 치료결과, 접촉자수
                Elements entity = patient.select("li>span");

                //8일 경과한 환자면 생략
                int year = Integer.parseInt(entity.get(0).text().split("-")[1]) < 1920 ? 2020 : 2021;
                Calendar c = string2CalendarFormatter(entity.get(1).text(), year);
                if(c!=null && ((Calendar.getInstance().getTimeInMillis() - c.getTimeInMillis()) / (24*60*60*1000) > 8)) break;

                //해쉬맵에 저장, 해당지역의 최근 확진일자는 객체 생성시에 고정(입력데이터가 최신순으로 정렬되어 있음)
                if(!districtList.containsKey(cityName+" "+entity.get(2).text())){
                    districtList.put(cityName+" "+entity.get(2).text(), new District(cityName+" "+entity.get(2).text(), c));
                }
                District cur = districtList.get(cityName+" "+entity.get(2).text());
                cur.confirmedPatientsList
                        .add(new ConfirmedPatient(c, districtList.get(cityName+" "+entity.get(2).text()), entity.get(3).text(), entity.get(4).text()));
                cur.lastlyConfirmedPatientNumber++;
            };
        }


    }

    private static void getNotIdentifiedContact(String cityName, String url) throws IOException {
        if(cityName.equals("부산")){
            Elements docElementList = Jsoup.connect(url)
                    .get()
                    .select("#contents > div > div > div > div > div > table > tbody > tr");
            for(Element contact : docElementList){
                Elements entity = contact.select("td");
                contactList.add(new NotIdentifiedContact(entity.get(4).text()));
            }
        }

    }

    private static Calendar string2CalendarFormatter(String str, int year){
        Calendar c = Calendar.getInstance();
        String[] slist = str.split("\\.");

        if(slist.length < 2 || slist[0].trim()=="" || slist[1].trim()=="") return null;
        else {
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, Integer.parseInt(slist[0].trim()) - 1);
            c.set(Calendar.DATE, Integer.parseInt(slist[1].trim()));

            return c;
        }
    }

    private static int string2Integer(String str){
        try{
            return Integer.parseInt(str);
        }catch(Exception e){
            return 0;
        }
    }

    private static class NotIdentifiedContact{
        String cityName;
        String districtName;
        String category;
        String storeName;
        String address;
        Calendar exposedBegin;
        Calendar exposedEnd;
        String result;

        NotIdentifiedContact(String dat){
            this.address = dat;
        }

        NotIdentifiedContact(String cn, String d, String cg, String s, String a, Calendar begin, Calendar end, String r){
            this.cityName = cn;
            this.districtName = d;
            this.category = cg;
            this.storeName = s;
            this.address = a;
            this.exposedBegin = begin;
            this.exposedEnd = end;
            this.result = r;
        }

    }

    private static class District{
        String name;
        Calendar lastlyConfirmedDate;
        int lastlyConfirmedPatientNumber;
        ArrayList<ConfirmedPatient> confirmedPatientsList;

        District(String n, Calendar c){
            this.name = n;
            lastlyConfirmedDate = c;
            lastlyConfirmedPatientNumber = 0;
            confirmedPatientsList = new ArrayList<>();

        }
    }

    private static class ConfirmedPatient{
        Calendar confirmedDate;
        District homeDistrict;
        String confirmedSource;
        String healthState;
        //int contactedPeopleNumber;

        ConfirmedPatient(Calendar c, District d, String cs, String h){
            this.confirmedDate = c;
            this.homeDistrict = d;
            this.confirmedSource = cs;
            this.healthState = h;
            //this.contactedPeopleNumber = cpn;

        }


    }

}