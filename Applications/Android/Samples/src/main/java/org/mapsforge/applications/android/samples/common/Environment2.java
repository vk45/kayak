package org.mapsforge.applications.android.samples.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.text.TextUtils;
import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

//import android.annotation.SuppressLint;

/**
 * 
 * Hilfsklasse mit mehreren Funktionsbl�cken, die auf die Problematik eingehen, 
 * dass viele moderne Android-Ger�te technisch zwei SD-Karten haben: 
 * <ul>
 * <li>Der "alte" Speicher, den Android als "External Memory" oder unter 
 * "mnt/sdcard" anspricht, ist dabei fest eingebaut; er hat zwar "sd" im Namen
 * und wird �ber Methoden mit "external" im Namen angesprochen, aber
 * das stimmt ansich nicht: Dieser Speicher ist nicht wechselbar und technisch
 * nicht als echte physische SD-Karte implementiert.
 * <li>Der bei diesen Ger�ten zug�ngliche Slot f�r eine microSD-Karte wird 
 * nicht �ber diese External-Methoden angesprochen. Bis Android 4.1 gibt es
 * keinen offiziellen Weg, drauf zuzugreifen. Daher liegt bei diesen Smartphones
 * die SD-Karte f�r Apps weitgehend brach. 
 * </ul>
 * <p>
 * Siehe dazu auch c't 22/12.
 * <p>Die Klasse selbst muss nicht instantiiert werden, sondern alle Methoden
 * sind static und beim Start der App wird automatisch {@link #rescanDevices()} 
 * aufgerufen, um die Liste der Devices zu erzeugen und somit eine echte
 * SD-Karte zu finden. Die Liste wird nicht automatisch aktualisiert, aber 
 * eine App kann rescanDevices() aufrufen. Auch ist ein BroadcastReceiver f�r 
 * automatische Updates implementiert, muss aber von der App
 * aufgerufen werden (siehe unten).
 * 
 * <h2>Die Funktionsbl�cke</h2>
 * <ol>
 * <li>Ob die bei vielen modernen Ger�ten (v.a. Tablets) vorhandene externe 
 * SD-Karte existiert, liefert {@link #isSecondaryExternalStorageAvailable()},
 * ob sie entfernbar ist, {@link #isSecondaryExternalStorageRemovable()}. 
 * 
 * <li>Um auf die Karte zugreifen zu k�nnen, sind von den "External"-Methoden 
 * (zum Zugriff auf die interne SD-Karte) �quivalente mit "Secondary" im 
 * Namen vorhanden. Entsprechend {@link android.os.Environment} sind das
 * {@link #getSecondaryExternalStorageDirectory()}, 
 * {@link #getSecondaryExternalStoragePublicDirectory(String)}, 
 * {@link #getSecondaryExternalStorageState()} 
 * und entsprechend {@link android.content.Context}
 * {@link #getSecondaryExternalCacheDir(android.content.Context)},
 * {@link #getSecondaryExternalFilesDir(android.content.Context, String)}.
 * 
 * <li>Um alles noch einen Schritt zu vereinfachen, gibt es den Methodensatz
 * auf Anregung von Sven Wiegand nochmal mit "Card" im Namen. Sie greifen
 * auf die externe SD-Karte zu und machen automatisch ein Fallback auf 
 *	/mnt/sdcard, falls keine externe vorhanden ist. Wenn ein sekund�rer Slot
 *	vorhanden ist, aber keine Karte eingesteckt, wird auch der Fallback
 *	benutzt. Ob die prim�re oder sekund�re Speicherkarte benutzt wird, l�sst 
 *	sich per {@link #isSecondaryExternalStorageAvailable()} herausfinden.
 *	<p>
 *	Die App muss nat�rlich �berpr�fen, ob die Daten an der Speicherstelle
 *	noch vorhanden sind; das muss aber sowieso sein.
 *	<p>
 *	Etwas problematisch: Wenn die App gestartet wird, bevor eine sekund�re
 *	Speicherkarte eingelegt wird, greifen diese Methoden auf diese Karte zu
 *	und finden dort die Daten nicht, obwohl sie auf dem internen Speicher
 *	liegen. Als Abhilfe sollte die App sich den Speicherort evtl. abspeichern.
 *	<p>
 * Implementiert sind: {@link #getCardDirectory()}, 
 * {@link #getCardPublicDirectory(String)}, {@link #getCardState()} , 
 * {@link #getCardCacheDir(android.content.Context)}, {@link #getCardFilesDir(android.content.Context, String)}.
 * 
 * <li>Weil das Auffinden der SD-Karte versagen kann oder weil Anwender 
 * ausw�hlen k�nnen sollen, wo sie ihre Daten speichern, ist auch eine 
 * Durchsuchfunktion f�r alle extern anbindbaren Ger�te (diese Zweit-SD, 
 * aber auch USB-Ger�te oder Kartenleser) vorhanden. Eine App kann
 * damit eine Liste aller verf�gbaren Speichermedien (inklusive der 
 * internen SD-Karte) anzeigen, zusammen mit deren Kapazit�t und 
 * freien Speicherplatz: {@link #getDevices(String, boolean, boolean)}.
 * Die zur�ckgegebene Liste kann man dann ausgeben; in 
 * {@link org.mapsforge.applications.android.samples.common.Device} gibts Methoden f�r Pfad, Name und Gr��e der Devices.
 *
 * <li>Zwei Hilfsmethoden, die die beide ab API9 und 13 in Environment
 * vorhandenen Funktionen zur Analyse des Ger�ts auch unter API8 nutzbar
 * machen, und die einige Besonderheiten von unseren Testger�ten
 * ber�cksichtigen: {@link #isExternalStorageEmulated()} und
 * {@link #isExternalStorageRemovable()}.
 *
 * <li>Hilfsmethoden zum Zugriff auf /data, /mnt/sdcard und ggf. die
 * externe Karte per {@link org.mapsforge.applications.android.samples.common.Device}-Interface: {@link #getPrimaryExternalStorage()},
 * {@link #getSecondaryExternalStorage()} und {@link #getInternalStorage()}
 * </ol>
 *
 * <h2>Aktualisierung der Ger�teliste</h2>
 * Falls eine App mitbekommen will, wenn Ger�te oder die SD-Karte
 * eingesteckt und entfernt werden, erstellt sie entweder einen eigenen
 * BroadcastReceiver oder (einfacher) �bergibt {@link #registerRescanBroadcastReceiver()}
 * ein Runnable.
 * <ul>
 * <li>Der eigene Receiver sollte in onReceive()  {@link #rescanDevices()} aufrufen.
 * Die Methode {@link #getRescanIntentFilter()} erzeugt den richtigen
 * IntentFilter f�r den Receiver.
 * <li>Der in registerRescanBroadcastReceiver() automatisch erzeugte Receiver
 * ruft erst rescanDevices() und danach den �bergebenen Runnable auf (der
 * auch null sein kann). registerReceiver wird auch automatisch aufgerufen,
 * aber an {@link android.content.Context#unregisterReceiver()} (�blicherweise in onDestroy())
 * muss man selbst denken.
 * </ul>
 * <h2>Background</h2>
 * Diese Liste aller mountbaren Ger�te (wozu diese Zweit-SDs z�hlt) l�sst sich
 * gl�cklicherweise bei allen bisher getesteten Ger�ten aus der Systemdatei
 * /system/etc/vold.fstab auslesen, einer Konfigurationsdatei eines
 * Linux-D�mons, der genau f�r das Einbinden dieser Ger�te zust�ndig ist.
 * Es mag Custom-ROMs geben, wo diese Methode nicht funktioniert.
 * <p>
 * Der MountPoint f�r die zweite SD-Karte stand bei allen bisher getesteten
 * Ger�ten direkt an erster Stelle dieser Datei, bei einigen nach /mnt/sdcard
 * an zweiter Stelle.
 * <p>
 * Andere Algorithmen zum Herausfinden des MountPoints sind noch nicht
 * implementiert; das w�rde ich erst machen, wenn diese Methode hier bei
 * einem Ger�t versagt. Denkbar w�re z.B. einfach eine Tabelle mit bekannten
 * MountPoints, die man der Reihe nach abklappert.
 * <p>
 *	Varianten des SD-Pfads sind:
 * <li>Asus Transformer		/Removable/MicroSD
 * <li>HTC Velocity LTE		/mnt/sdcard/ext_sd
 * <li>Huawei MediaPad		/mnt/external
 * <li>Intel Orange					/mnt/sdcard2
 * <li>LG Prada						/mnt/sdcard/_ExternalSD
 * <li>Motorola Razr			/mnt/sdcard-ext
 * <li>Motorola Xoom			/mnt/external1
 * <li>Samsung Note			/mnt/sdcard/external_sd (und Pocket und Mini 2)
 * <li>Samsung Note II			/storage/extSdCard
 * <li>Samsung S3					/mnt/extSdCard
 *  <p>
 *  Einige Hersteller h�ngen die SD-Karte unter /mnt ein, andere in die
 *  interne Karte /mnt/sdcard (was dazu f�hrt, dass einige Unterverzeichnisse
 *  von /mnt/sdcard gr��er sind als der gesamte interne Speicherbereich ;-),
 *  wieder andere in ein anderes Root-Verzeichnis.
 *
 *  @author J�rg Wirtgen (jow@ct.de)
 *  @version 1.1
 */

public class Environment2  {
	private static final String TAG = "Environment2";
	private static final boolean DEBUG = true;

	private static ArrayList<Device> mDeviceList = null;
	private static boolean mExternalEmulated = false;
	protected static Device mPrimary = null;
	private static Device mSecondary = null;

	static {
		rescanDevices();
	}


	/**
	 * Fragt ab, ob die Zweit-SD vorhanden ist. Der genauere Status kann
	 * danach per {@link #getSecondaryExternalStorageState()} abgefragt werden.
	 * @return true, wenn eine Zweit-SD vorhanden und eingelegt ist,
	 * 		false wenn nicht eingelegt oder kein Slot vorhanden
	 */
	public static boolean isSecondaryExternalStorageAvailable() {
		boolean result = mSecondary!=null && mSecondary.isAvailable();
		if (result){
			// on a Medion Lifetab with 4.0.3  the entry may point to the usb-storage
			// if the usb-store is not put in , there is a empty directory in /mnt/usbdrive
			// 2014_01_29
			File theDir = mSecondary.getFile();
			String pathname = theDir.getAbsolutePath();
			if (pathname.contains("usbdrive")){
				// we test if the /mnt/usbdrive is writable
				boolean isWritable = theDir.canWrite();
				return isWritable;
			}

		}
		return result;
	}


	/**
	 * Zeigt an, ob die Zweit-SD entfernt werden kann; derzeit kenne ich kein
	 * Ger�t, bei dem die fest eingebaut w�re, also immer true
	 * @return true
	 * @throws NoSecondaryStorageException falls keine Zwei-SD vorhanden
	 * @see #isSecondaryExternalStorageAvailable()
	 */
	public final static boolean isSecondaryExternalStorageRemovable() throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		return true;
	}


	/**
	 * Ein Zeiger auf die Zweit-SD, falls gefunden
	 * @return das Verzeichnis der Zwei-SD
	 * @throws NoSecondaryStorageException wenn keine Zwei-SD vorhanden oder nicht eingelegt
	 * @see #isSecondaryExternalStorageAvailable()
	 */
	public static File getSecondaryExternalStorageDirectory() throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		return mSecondary.getFile();
	}


	/**
	 * Liefert den Status einer zweiten SD-Karte oder wirt eine Exception
	 * <p>
	* Zum Schreiben auf die Karte ist eine Permission notwendig:
	* <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
	*	<p>
	* TODO ab JellyBean 4.1 soll es auch eine Read-Permission geben?!
	 * @return einer von den drei in Environment definierten States
	 * 	MEDIA_MOUNTED, _MOUNTED_READ_ONLY und _REMOVED
	 * @throws NoSecondaryStorageException wenn kein zweiter SD-Slot vorhanden
	 *
	 * @see #isSecondaryExternalStorageAvailable()
	 */
	public static String getSecondaryExternalStorageState() throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		if (mSecondary.isAvailable())
			return mSecondary.isWriteable() ? Environment.MEDIA_MOUNTED : Environment.MEDIA_MOUNTED_READ_ONLY;
		else
			return Environment.MEDIA_REMOVED;
	}


	/**
	 * Gibt die Public-Directories auf der Zweit-SD zur�ck; legt
	 * sie (wie die Environment-Methode) nicht an.
	 * @param s ein String aus Environment.DIRECTORY_xxx,
	 * darf nicht null sein. (Funktioniert auch mit anderen Pfadnamen
	 * und mit verschachtelten)
	 * @return ein File dieses Verzeichnisses. Wenn Schreibzugriff gew�hrt,
	 * wird es angelegt, falls nicht vorhanden
	 * @throws NoSecondaryStorageException falls keine Zweit-SD vorhanden
	 */
	public static File getSecondaryExternalStoragePublicDirectory(String s) throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		if (s==null) throw new IllegalArgumentException("s darf nicht null sein");
		return getSecondaryDirectoryLow(s, false);
	}


	/**
	 * Nachbau der Context-Methode getExternalFilesDir(String) mit zwei Unterschieden:
	 * <ol>
	 * <li>man muss halt Context �bergeben
	 * <li>das Verzeichnis wird bei der App-Deinstallation nicht gel�scht
	 * </ol>
	 * @param context der Context der App; ben�tigt, um den Pfadnamen auszulesen
	 * @param s ein String aus Environment.DIRECTORY_xxx, kann aber auch
	 * 		ein anderer (verschachtelter) sein oder null
	 * @return das Verzeichnis. Wird angelegt, wenn man Schreibzugriff hat
	 * @throws NoSecondaryStorageException falls keine Zwei-SD vorhanden
	 */
	public static File getSecondaryExternalFilesDir(Context context, String s) throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		if (context==null) throw new IllegalArgumentException("context darf nicht null sein");
		String name = "/Android/data/" + context.getPackageName() + "/files";
		if (s!=null) name += "/" + s;
		return getSecondaryDirectoryLow(name, true);
	}


	public static File getSecondaryExternalCacheDir(Context context) throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		if (context==null) throw new IllegalArgumentException("context darf nicht null sein");
		String name = "/Android/data/" + context.getPackageName() + "/cache";
		return getSecondaryDirectoryLow(name, true);
	}


	/**
	 * interne Routine ohne Fehler�berpr�fung und mit M�glichkeit, den Pfad zu erstellen -- oder auch nicht
	 * @param s der Pfad, der an External angeh�ngt wird, darf nicht null sein
	 * @param create Verzeichnis erzeugen oder nicht
	 * @return das angeforderte Verzeichnis
	 */
	private static File getSecondaryDirectoryLow(String s, boolean create) {
		File f = new File(mSecondary.getMountPoint()+"/"+s);
		if (DEBUG) Log.v(TAG, "getLow "+f.getAbsolutePath()+" e:"+f.exists()+" d:"+f.isDirectory()+" w:"+f.canWrite() );
		if (create && !f.isDirectory() && mSecondary.isWriteable())
			// erzeugen, falls es nicht existiert und Schreibzugriff auf die SD vorhanden
			f.mkdirs();
		return f;
	}

/*
	 * Implementiert sind: {@link #getCardDirectory()},
	 * {@link #getCardPublicDirectory(String)}, {@link #getCardStorageState()} ,
	 * {@link #getCardCacheDir(Context)}, {@link #getCardFilesDir(Context, String)}.
*/
	public static File getCardDirectory() {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalStorageDirectory();}
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return Environment.getExternalStorageDirectory();
	}

	/*public static File getCardPublicDirectory(String dir) {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalStoragePublicDirectory(dir);}
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return Environment.getExternalStoragePublicDirectory(dir);
	}*/

	public static String getCardState() {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalStorageState();}
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return Environment.getExternalStorageState();
	}

	/*public static File getCardCacheDir(Context ctx) {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalCacheDir(ctx);}
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return ctx.getExternalCacheDir();
	}
*/
	/*public static File getCardFilesDir(Context ctx, String dir) {
		if (isSecondaryExternalStorageAvailable())
			try {return getSecondaryExternalFilesDir(ctx, dir);}
			catch (NoSecondaryStorageException e) {throw new RuntimeException("NoSecondaryException trotz Available"); }
		else
			return ctx.getExternalFilesDir(dir);

	}*/



	/**
	 * Alternative zu {@code Environment#isExternalStorageEmulated() },
	 * die ab API8 funktioniert. Wenn true geliefert wird, handelt es sich
	 * um ein Ger�t mit "unified memory", bei dem /data und /mnt/sdcard
	 * auf denselben Speicherbereich zeigen. App2SD ist dann deaktiviert,
	 * und zum Berechnen des freien Speichers darf man nicht den der beiden
	 * Partitionen addieren, sondern nur einmal z�hlen.
	 *
	 * @return true, falls /mnt/sdcard und /data auf den gleichen Speicherbereich zeigen;
	 * 	false, falls /mnt/sdcard einen eigenen (nicht notwendigerweise auf einer
	 * externen SD-Karte liegenden!) Speicherbereich beschreibt
	 *
	 * @see #isExternalStorageRemovable()
	 */
	public static boolean isExternalStorageEmulated() {
		return mExternalEmulated;
	}


	/**
	 * Alternative zu {@link android.os.Environment#isExternalStorageRemovable()},
	 * die ab API8 funktioniert. Achtung: Die Bedeutung ist eine subtil andere
	 * als beim Original-Aufruf. Hier geht es (eher zu Hardware-Diagnosezwecken)
	 * darum, ob /mnt/sdcard eine physische Karte ist, die der Nutzer
	 * herausnehmen kann. Der Original-Aufruf liefert true, wenn es sein kann,
	 * dass auf /mnt/sdcard nicht zugegriffen werden kann, was auch bei fest
	 * eingebauten Karten der Fall sein kann, und zwar wenn sie per USB
	 * an einen PC freigegeben werden k�nnen und w�hrenddessen nicht
	 * f�r Android im Zugriff stehen.
	 *
	 * @return true, falls /mnt/sdcard auf einer entnehmbaren
	 * physischen Speicherkarte liegt
	 * 	false, falls das ein fest verl�teter Speicher ist - das hei�t nicht,
	 * dass immer auf den Speicher zugegriffen werden kann, ein
	 * Status-Check muss dennoch stattfinden (anders als
	 * bei Environment.isExternalStorageRemovable())
	 *
	 * @see #isExternalStorageEmulated()
	 */
	public static boolean isExternalStorageRemovable() {
		return mPrimary.isRemovable();
	}



	/**
	 * Hilfe zum Erstellen eines BroadcastReceivers: So muss der passende
	 * IntentFilter aussehen, damit der Receiver alle �nderungen mitbekommt.
	 * Wenn man einen eigenen Receiver programmiert statt
	 * {@link Environment2#registerRescanBroadcastReceiver(android.content.Context, Runnable)}
	 * zu nutzen, sollte man dort {@link Environment2#rescanDevices()} aufrufen.
	 * @return einen IntentFilter, der auf alle Intents h�rt, die einen Hardware-
	 * 		und Kartenwechsel anzeigen
	 * @see android.content.IntentFilter
	 */
	public static IntentFilter getRescanIntentFilter() {
		if (mDeviceList==null) rescanDevices();
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL); // rausgenommen
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED); // wieder eingesetzt
		filter.addAction(Intent.ACTION_MEDIA_REMOVED); // entnommen
		filter.addAction(Intent.ACTION_MEDIA_SHARED); // per USB am PC
		// geht ohne folgendes nicht, obwohl das in der Doku nicht so recht steht
		filter.addDataScheme("file");

		/*
		 * die folgenden waren zumindest bei den bisher mit USB getesteten
		 * Ger�ten nicht notwendig, da diese auch bei USB-Sticks und externen
		 * SD-Karten die ACTION_MEDIA-Intents abgefeuert haben
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		 */
		return filter;
	}


	/**
	 * BroadcastReceiver, der einen Rescan durchf�hrt und (als Callback) das
	 * �bergebene Runnable aufruft. Muss mit unregisterReceiver freigegeben
	 * werden; daf�r ist der Aufrufer verantwortlich. Das Registrieren des
	 * Receivers wird hier schon durchgef�hrt (mit getRescanIntentFilter)
	 * <p>
	 * Das geht dann (z.B. in onCreate() ) so: <pre>
		BroadcastReceiver mRescanReceiver
		= Environment2.registerRescanBroadcastReceiver(this, new Runnable() {
	 		public void run() {
	 			auszuf�hrende Befehle
	 		}
	 	});</pre>
	 * <p>
	 * und sp�ter (z.B. in onDestroy() ): {@code unregisterReceiver(mRescanReceiver);}
	 * <p>
	 * Der hier implementierte Receiver macht nichts anderes als {@link #rescanDevices() }
	 * und dann den Runnable aufzurufen.
	 * @param context der Context, in dem registerReceiver aufgerufen wird
	 * @param r der Runnable, der bei jedem An- und Abmelden von Devices
	 * 		ausgef�hrt wird; kann auch null sein
	 * @return der BroadcastReceiver, der sp�ter unregisterReceiver �bergeben
	 * 		werden muss. Registriert werden muss er nicht, das f�hrt die
	 * 		Methode hier durch.
	 * @see #getRescanIntentFilter()
	 * @see android.content.BroadcastReceiver
	 */
	public static BroadcastReceiver registerRescanBroadcastReceiver(Context context, final Runnable r) {
		if (mDeviceList==null) rescanDevices();
		BroadcastReceiver br = new BroadcastReceiver() {
			@Override public void onReceive(Context context, Intent intent) {
				if (DEBUG) Log.i(TAG, "Storage: "+intent.getAction()+"-"+intent.getData());
				rescanDevices();
				if (r!=null) r.run();
			}
		};
		context.registerReceiver(br, getRescanIntentFilter());
		return br;
	}


	/**
	 * Sucht das Ger�t nach internen und externen Speicherkarten und USB-Ger�ten
	 * ab. Wird automatisch beim App-Start aufgerufen (in einem static-initializer)
	 * und kann sich per BroadcastReceiver selbst aktualisieren. Muss also
	 * eigentlich nie von der App aufgerufen werden, au�er in einem Fall:
	 * Wenn man selbst einen BroadcastReceiver zum Erkennen von Wechseln
	 * bei Devices schreibt, sollte in dessen onReceive() diese Methode
	 * aufgerufen werden.
	 *
	 * @see Environment2#registerRescanBroadcastReceiver(android.content.Context, Runnable)
	 */
	//@SuppressLint("NewApi")
	public static void rescanDevices() {
		mDeviceList = new ArrayList<Device>(10);
		mPrimary = new Device().initFromExternalStorageDirectory();

		// vold.fstab lesen; TODO bei Misserfolg eine andere Methode
		if (!scanVold("vold.fstab")) scanVold("vold.conf");

    	// zeigen /mnt/sdcard und /data auf denselben Speicher?
    	/*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
    		mExternalEmulated = Environment.isExternalStorageEmulated();
    	} else {
    		// vor Honeycom gab es den unified memory noch nicht
    		mExternalEmulated = false;
    	}*/
    	mExternalEmulated = false;
		// Pfad zur zweiten SD-Karte suchen; bisher nur Methode 1 implementiert
		// Methode 1: einfach der erste Eintrag in vold.fstab, ggf. um ein /mnt/sdcard-Doppel bereinigt
		// Methode 2: das erste mit "sd", falls nicht vorhanden das erste mit "ext"
		// Methode 3: das erste verf�gbare
		if (mDeviceList.size()==0) {
			mSecondary = null;
		} else {
			mSecondary = mDeviceList.get(0);
			// Hack
			if (mPrimary.isRemovable()) Log.w(TAG, "isExternStorageRemovable overwrite (secondary sd found) auf false");
			mPrimary.setRemovable(false);
		}

		// jetzt noch Name setzen TODO in strings.xml
		mPrimary.setName( mPrimary.isRemovable() ? "SD-Card" : "intern" );
	}


	/**
	 * Die vold-Konfigurationsdatei auswerten, die �blicherweise
	 * in /system/etc/ liegt.
	 * @param name ein String mit dem Dateinamen (vold.fstab oder vold.conf)
	 * @return true, wenn geklappt hat; false, wenn Datei nicht (vollst�ndig)
	 * 		gelesen werden konnte. Falls false, werden die bisher gelesenen
	 * 		Devices nicht wieder gel�scht, sondern bleiben in der Liste
	 * 		enthalten. Bisher ist mir aber noch kein Ger�t untergekommen,
	 * 		bei dem dieser Trick nicht funktioniert hat.
	 */
	private static boolean scanVold(String name) {
		String s, f;
		boolean prefixScan = true; // sdcard-Prefixes
		SimpleStringSplitter sp = new SimpleStringSplitter(' ');
    	try {
    		BufferedReader buf = new BufferedReader(new FileReader(Environment.getRootDirectory().getAbsolutePath()+"/etc/"+name), 2048);
    		s = buf.readLine();
    		while (s!=null) {
    			sp.setString(s.trim());
    			f = sp.next(); // dev_mount oder anderes
        		if ("dev_mount".equals(f)) {
        			Device d = new Device();
        			d.initFromStringSplitter(sp);

        			if (TextUtils.equals(mPrimary.getMountPoint(), d.getMountPoint())) {
        				// ein wenig Spezialkrams �ber /mnt/sdcard herausfinden

        				// wenn die Gingerbread-Funktion isExternalStorageRemovable nicht da ist, diesen Hinweis nutzen
        				//if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD)
        					mPrimary.setRemovable(true);
        					// dann ist auch der Standard-Eintrag removable
        					// eigentlich reicht das hier nicht, denn die vold-Eintr�ge f�r die prim�re SD-Karte sind viel komplexer,
        					// oft steht da was von non-removable. Doch diese ganzen propriet�ren Klamotten auszuwerden,
        					// w�re viel zu komplex. Ein gangbarer Kompromiss scheint zu sein, sich ab 2.3 einfach auf
        					// isExternalStorageRemovable zu verlassen, was schon oben in Device() gesetzt wird. Bei den
        					// bisher aufgetauchten Ger�ten mit 2.2 wiederum scheint der Hinweis in vold zu klappen.vccfg

        				// z.B. Galaxy Note h�ngt "encryptable_nonremovable" an
        				while (sp.hasNext()) {
        					f = sp.next();
        					if (f.contains("nonremovable")) {
        						mPrimary.setRemovable(false);
        						Log.w(TAG, "isExternStorageRemovable overwrite ('nonremovable') auf false");
        					}
        				}
        				prefixScan = false;
        			} else
        				// nur in Liste aufnehmen, falls nicht Dupe von /mnt/sdcard
        				mDeviceList.add(d);

        		} else if (prefixScan) {
        			// Weitere Untersuchungen nur, wenn noch vor sdcard-Eintrag
        			// etwas unsauber, da es eigentlich in {} vorkommen muss, was ich hier nicht �berpr�fe

        			if ("discard".equals(f)) {
        				// manche (Galaxy Note) schreiben "discard=disable" vor den sdcard-Eintrag.
        				sp.next(); // "="
        				f = sp.next();
        				if ("disable".equals(f)) {
        					mPrimary.setRemovable(false);
        					Log.w(TAG, "isExternStorageRemovable overwrite ('discard=disable') auf false");
        				} else if ("enable".equals(f)) {
        					// ha, denkste...  bisher habe ich den Eintrag nur bei zwei Handys gefunden, (Galaxy Note, Galaxy Mini 2), und
        					// da stimmte er *nicht*, sondern die Karten waren nicht herausnehmbar.
        					// mPrimary.mRemovable = true;
        					Log.w(TAG, "isExternStorageRemovable overwrite overwrite ('discard=enable'), bleibt auf "+mPrimary.isRemovable());
        				} else
        					Log.w(TAG, "disable-Eintrag unverst�ndlich: "+f);
        			}

        		}
    			s = buf.readLine();
    		}
    		buf.close();
    		return true;
    	} catch (Exception e) {
    		Log.e(TAG, "kann "+name+" nicht lesen: "+e.getMessage());
    		return false;
    	}
	}


	/**
	 * Liste aller gefundener Removable-Ger�te zusammenstellen. Die Liste kann
	 * nach Device-Namen und weiteren Parametern eingeschr�nkt werden.
	 *
	 * @param key ein String zum Einschr�nken der Liste. Findet nur die Devices
	 * 		mit dem String in getName() oder alle, falls null.
	 * @param available ein Boolean zum Beschr�nken der Liste auf vorhandene
	 * 		(eingesteckte) Ger�te. false findet alle, true nur diejenigen, die eingesteckt sind
	 * @param intern ein Boolean, der bestimmt, ob der interne Speicher (/mnt/sdcard)
	 * 		mit in die Liste �bernommen wird (unter Ber�cksichtigung von available,
	 * 		aber nicht key).
	 * @return ein Array mit allen {@link org.mapsforge.applications.android.samples.common.Device}, die den Suchkriterien entsprechen
	 */
	public static Device[] getDevices(String key, boolean available, boolean intern) {
		if (key!=null) key = key.toLowerCase();
		ArrayList<Device> temp = new ArrayList<Device>(mDeviceList.size());
		if (intern && ( !available || mPrimary.isAvailable())) temp.add(mPrimary);
		for (Device d : mDeviceList) {
			if ( ((key==null) || d.getName().toLowerCase().contains(key)) && (!available || d.isAvailable()) ) temp.add(d);
		}
		return temp.toArray(new Device[temp.size()]);
	}
	

	public static Device getPrimaryExternalStorage() {
		return mPrimary;
	}
	
	
	public static Device getSecondaryExternalStorage() throws NoSecondaryStorageException {
		if (mSecondary==null) throw new NoSecondaryStorageException();
		return mSecondary;
	}
	
	
	public static Device getInternalStorage() {
		return new Device().initFromDataDirectory();
	}
	
	
}
