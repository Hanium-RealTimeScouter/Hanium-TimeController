package scouter.plugin.server.hanium.timecontroller;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

import scouter.lang.pack.AlertPack;
import scouter.lang.pack.Pack;
import scouter.lang.plugin.PluginConstants;
import scouter.lang.plugin.annotation.ServerPlugin;
import scouter.server.Logger;
import scouter.server.core.AgentManager;

/**
 * <p> <b> [2017.05.24] 한이음 실시간 성능 모니터링 시스템 </b> </p>
 * 각 오브젝트 별로 에러가 얼만큼의 시간동안 지속되는지 체크하기 위한 클래스.<br>
 * Key = Integer(오브젝트 해시), Value =  Long[] {최초 에러발생시간, 임계시간, 에러 발생 횟수, 안정시간, 마지막 에러발생시간}의 HashMap을 
 * static으로 구현하여 서버 코드 모든 곳에서 사용할 수 있도록 함
 * @author occidere
 */
public class ObjectContainer {
	
	private static final String NEW_LINE = System.getProperty("line.separator");
	
	/* static way 이기 때문에 객체생성 금지 */
//	private ObjectContainer(){}
	
	/* 객체의 해시값(int)을 key로 하고, long [] 배열을 values로 하는 해시맵
	 * 배열에는 다음과 같은 값들이 저장된다
	 * 
	 * [0]: ERR_START_TIME = 최초 에러 발생 시간
	 * [1]: ERR_LIMIT_TIME = 에러발생 임계시간 (최초 에러 발생 시간으로부터 이 시간동안 에러가 지속되면 메일을 보낸다)
	 * [2]: ERR_COUNT = 최초 에러 발생 시간 ~ 임계시간 사이의 누적 에러 발생 횟수
	 * [3]: ERR_SAFE_COUNT = 안정시간 (마지막 에러 발생 시간으로부터 이 시간동안 에러가 발생하지 않으면 모든 값을 초기화한다)
	 * [4]: ERR_LAST_TIME = 마지막 에러 발생 시간
	 * 
	 */
	//private static transient HashMap<Integer, long[]> objContainer = new HashMap<Integer, long[]>();
	
	private static transient HashMap<Integer, Container> objContainer = new HashMap<Integer, Container>();
	
	//최초 에러 발생 시간
	public static final int ERR_START_TIME = 0;
	//에러발생 임계시간(최초 에러 발생 시간으로부터 이 시간동안 에러가 지속되면 메일을 보낸다)
	public static final int ERR_LIMIT_TIME = 1;
	//최초 에러 발생 시간 ~ 임계시간 사이의 누적 에러 발생 횟수
	public static final int ERR_COUNT = 2;
	//안정시간 (마지막 에러 발생 시간으로부터 이 시간동안 에러가 발생하지 않으면 모든 값을 초기화한다)
	public static final int ERR_SAFE_TIME = 3;
	//마지막 에러 발생 시간
	public static final int ERR_LAST_TIME = 4;
	
	//에러 발생 객체 이름
	public static final int ERR_OBJ_NAME = 10;
	
	/**
	 * 특정 오브젝트의 에러 발생 정보를 초기화 하는 메서드.<br>
	 * 최초 에러 발생 시간과 임계시간, 누적 에러 발생 횟수를 배열 형태로 해시맵에 저장한다.<br>
	 * @param objHash 대상 오브젝트의 해시값
	 * @param startTimeMillis 최초 에러 발생 시간
	 * @param limitTimeMillis 임계시간
	 * @param safeTimeMillis 안정시간
	 */
	public synchronized void init(String name, int objHash, long startTimeMillis, long limitTimeMillis, long safeTimeMillis){
		long values[] = new long[5];
		
		values[ERR_START_TIME] = startTimeMillis; //최초 에러 발생 시간
		values[ERR_LIMIT_TIME] = limitTimeMillis; //에러 임계시간
		values[ERR_COUNT] = 1; //누적 에러 횟수
		values[ERR_SAFE_TIME] = safeTimeMillis; //에러 안전시간
		values[ERR_LAST_TIME] = startTimeMillis; //마지막 에러 발생시간
		
		objContainer.put(objHash, new Container(name, values));
	}
	
	/**
	 * 특정 오브젝트의 타입에 대한 값을 설정함<p>
	 * <li><b> ERR_START_TIME </b> = 최초 에러 발생 시간
	 * <li><b> ERR_LIMIT_TIME </b> = 에러발생 임계시간 (최초 에러 발생 시간으로부터 이 시간동안 에러가 지속되면 메일을 보낸다)
	 * <li><b> ERR_COUNT </b> = 최초 에러 발생 시간 ~ 임계시간 사이의 누적 에러 발생 횟수
	 * <li><b> ERR_SAFE_COUNT </b> = 안정시간 (마지막 에러 발생 시간으로부터 이 시간동안 에러가 발생하지 않으면 모든 값을 초기화한다)
	 * <li><b> ERR_LAST_TIME </b> = 마지막 에러 발생 시간
	 * @param objHash 특정 오브젝트의 해시값 
	 * @param TYPE 값을 설정할 타입
	 * @param value 타입에 대해 설정할 값
	 */
	public synchronized void setValue(int objHash, final int TYPE, long value){
		objContainer.get(objHash).getValues()[TYPE] = value;
	}
	
	/**
	 * 특정 오브젝트의 타입에 대한 값을 가져옴<p>
	 * <li><b> ERR_START_TIME </b> = 최초 에러 발생 시간
	 * <li><b> ERR_LIMIT_TIME </b> = 에러발생 임계시간 (최초 에러 발생 시간으로부터 이 시간동안 에러가 지속되면 메일을 보낸다)
	 * <li><b> ERR_COUNT </b> = 최초 에러 발생 시간 ~ 임계시간 사이의 누적 에러 발생 횟수
	 * <li><b> ERR_SAFE_COUNT </b> = 안정시간 (마지막 에러 발생 시간으로부터 이 시간동안 에러가 발생하지 않으면 모든 값을 초기화한다)
	 * <li><b> ERR_LAST_TIME </b> = 마지막 에러 발생 시간
	 * @param objHash 특정 오브젝트의 해시값
	 * @param TYPE 값을 가져올 타입
	 * @return
	 */
	public long getValue(int objHash, final int TYPE){
		return objContainer.get(objHash).getValues()[TYPE];
	}
	
	/**
	 * 특정 오브젝트가 기록되어 있는지 확인하는 메서드.<br>
	 * @param objHash 오브젝트의 해시 값
	 * @return 기록되어 있으면 true, 기록되어 있지 않으면 false
	 */
	public boolean contains(int objHash){
		return objContainer.containsKey(objHash);
	}
	
	/**
	 * 특정 오브젝트를 제거하는 메서드<br>
	 * @param objHash 제거할 오브젝트의 해시 값
	 * @return 기존에 있었던 오브젝트가 정상적으로 제거가 되면, 해당 오브젝트의 모든 정보가 long[] 배열로 반환.<br>
	 * 원래 없었던 오브젝트를 제거한다면 null 반환
	 */
	public synchronized long[] remove(int objHash){
		return objContainer.remove(objHash).getValues();
	}
	
	/**
	 * 특정 오브젝트의 정보가 담긴 배열을 가져오는 메서드<br>
	 * @param objHash 정보를 가져올 대상 오브젝트의 해시 값
	 * @return 해당 오브젝트의 정보가 담긴 배열
	 */
	public long[] getAllValues(int objHash){
		return objContainer.get(objHash).getValues();
	}
	
	/**
	 * 지정한 임계시간을 초과하였는지 확인하는 메서드<br>
	 * @param objHash 임계시간 초과여부를 검사할 메서드
	 * @param limitTimeMillis 에러발생이 지속될 임계시간
	 * @return 지정한 임계시간을 넘겨서 에러가 계속 발생중이라면 true<br>
	 * 임계시간 미만으로 에러가 발생중이라면 false
	 */
	public boolean exceedLimitTime(int objHash){
		/* 에러발생 시간 검사할 객체가 map에 기록되지 않은 객체라면 예외발생 */
		if(contains(objHash) == false){
			throw new NullPointerException("No such object hash value: " + objHash);
		}
		
		long curTime = System.currentTimeMillis(); //현재 시간
		long values[] = getAllValues(objHash); //해당 오브젝트의 모든 정보를 가져옴
		long elapsedTime; //최초 오류 발생으로부터 경과된 시간
		
		/* 누적 에러발생시간 = 현재시간 - 객체의 초기 에러발생시간 */
		elapsedTime = curTime - values[ERR_START_TIME];
		
		return elapsedTime > values[ERR_LIMIT_TIME];
	}
	
	/**
	 * foreach문을 돌면서 안전시간동안 에러가 발생하지 않은 오브젝트들을 map에서 제거함.
	 * @param meaninglessPack 스카우터의 BuiltInPluginManager에서 load하기 위한 의미없는 pack
	 */
	@ServerPlugin(PluginConstants.PLUGIN_SERVER_COUNTER)
	public void gc(Pack meaninglessPack){
		
		long values[], curTime = System.currentTimeMillis();
		//foreach 돌면서 안전시간동안 에러가 발생하지 않은 오브젝트를 map에서 제거함.
		for(Entry<Integer, Container> obj : objContainer.entrySet()){
			values = obj.getValue().getValues();
			
			Container con = obj.getValue();
			long diff = curTime - con.getValues()[ERR_LAST_TIME], pivot = con.getValues()[ERR_SAFE_TIME];
			Logger.println(String.format("<테스트>이름: %s\n시간차: %d > %d ?\n", con.getName(), diff, pivot));
			
			//최종 에러발생 시간으로부터 safe time 이상의 시간이 지났으면 gc
			if(curTime - values[ERR_LAST_TIME] > values[ERR_SAFE_TIME]){
				String name = AgentManager.getAgentName(obj.getKey()) == null ? "N/A" : AgentManager.getAgentName(obj.getKey());
				Logger.println("[한이음] Name: " + obj.getValue().getName());
				Logger.println( String.format("%s 객체가 안정되어 GC에 의해 에러 컨테이너에서 제거됨", name));
				Logger.println( String.format("startTime=%d, limitTime=%d, count=%d, safeTime=%d, lastTime=%d",
						values[ERR_START_TIME], values[ERR_LIMIT_TIME], values[ERR_COUNT], values[ERR_SAFE_TIME], values[ERR_LAST_TIME]));
				Logger.println("");
				remove(obj.getKey());
			}
		}
	}
	
	/**
	 * 각 알림 플러그인에서 일일히 설정하는 번거로움을 없애기 위해 만든 메서드.<br>
	 * AlertPack과 임계시간, 안전시간만 매개변수로 입력하면 알아서 조건에 맞춰 알림을 보내고 초기화 과정을 진행한다.<br>
	 * @param ap 객체정보를 담은 AlertPack
	 * @param limitTime 각 객체별 임계시간
	 * @param safeTime 각 객체별 안전시간
	 * @return 메일을 보냈으면 true, 안보냈으면 false
	 */
	public boolean sendAlert(AlertPack ap, String name, long limitTime, long safeTime){
		String additionalMessage = "";
    	long values[], curTime = ap.time; //현재시간;
    	final long A_MINUTE = 60000; //1분
    	int objHash = ap.objHash; //객체의 에러 발생 시간
    	
    	//본문에 표시될 에러 발생 시간 포맷
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 HH시 mm분 ss초");
    	
    	/* 객체의 에러 발생 임계치 초과 여부 체크 */
    	if(contains(objHash)){

    		values = getAllValues(objHash); //해당 오브젝트의 값
    		
    		//임계시간 초과 시 메세지 추가 후 메일 전송
    		if(exceedLimitTime(objHash)){
    			
    			additionalMessage = String.format(NEW_LINE +
    					"[위 험] : 에러가 %d분 동안 지속 발생중입니다! (제한시간: %d분)" + NEW_LINE +
    					"          최초 발생시간: %s"+ NEW_LINE +
    					"          누적 에러 발생 횟수: %d 회"+ NEW_LINE +
    					"(테스트: %s)" + NEW_LINE,
    					(curTime - values[ERR_START_TIME]) / A_MINUTE,
    					(values[ERR_LIMIT_TIME]) / A_MINUTE, 
    					sdf.format(new Date(values[ERR_START_TIME])),
    					values[ERR_COUNT],
    					name);
    			
    			ap.message += additionalMessage; //추가 메세지를 원본 메세지 뒤에 붙임
    			
    			/* 임계시간 초과 시 메일 보내고 객체 삭제 -> 정책별로 상이 */
    			remove(objHash);
    		}
    		else{
    			//아직 임계시간 안지났으면 에러발생횟수 + 1 갱신,
    			//마지막 에러 발생시간 갱신 후 종료
    			setValue(objHash, ERR_COUNT, values[ERR_COUNT] + 1);
    			setValue(objHash, ERR_LAST_TIME, curTime);
    			return false;
    		}
    	}
    	else{ //객체가 없다 == 첫 에러 발생
    		
    		// limitTime 동안 계속 에러나면 메일보낸다
    		// safeTime 간 에러가 한번도 없으면 객체 삭제
    		init(name, objHash, curTime, limitTime, safeTime);
    		return false; //첫 에러 발생에 대한 정보 등록 후 종료
    	}
    	return true;
	}
	
	/**
	 * 저장된 오브젝트 정보들을 모두 삭제하는 메서드<br>
	 */
	public void clear(){
		objContainer.clear();
	}
}