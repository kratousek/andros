package com.example.main;

import static androidx.core.content.PackageManagerCompat.LOG_TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.main.databinding.FragmentFirstBinding;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import com.ftdi.javad2xxdemo.uHer;
import com.ftdi.javad2xxdemo.pars;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


import android.app.Activity;


public class FirstFragment extends Fragment{

    private FragmentFirstBinding binding;
    private int mCount = 0;
    private TextView mShowCount;

    // minimalni setting k provozu FTDI
    static Context DeviceUARTContext;
    D2xxManager ftdid2xx = null;
    FT_Device ftDev = null;
    private int DevCount =-1;
    private int currentIndex = -1;
    private boolean bReadThreadGoing;
    private int fAddr;
    private int progressBarStatus=0;
    private Handler progressBarHandler = new Handler();

    private int openIndex = 0;
    boolean uart_configured = false;
    public com.ftdi.javad2xxdemo.uHer read_thread;


    public void notifyUSBDeviceAttach()
    {
        createDeviceList();
    }
    public void notifyUSBDeviceDetach()
    {
        disconnectFunction();
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        mShowCount = binding.showCount;

        try {
            ftdid2xx = D2xxManager.getInstance(getContext());
        } catch (D2xxManager.D2xxException ex) {
            ex.printStackTrace();
        }

        if(!ftdid2xx.setVIDPID(0x0403, 0xada1))
            Log.i("ftd2xx-java","setVIDPID Error");

        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        filter.setPriority(500);

        DeviceUARTContext = getContext();
        int tempDevCount = ftdid2xx.createDeviceInfoList(DeviceUARTContext);
        return binding.getRoot();

    }


    public void disconnectFunction()
    {
        DevCount = -1;
        currentIndex = -1;
        bReadThreadGoing = false;
        try {
            Thread.sleep(50);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(ftDev != null)
        {
            synchronized(ftDev)
            {
                if( true == ftDev.isOpen())
                {
                    ftDev.close();
                }
            }
        }
    }

    public void connectFunction()
    {
        int tmpProtNumber = openIndex + 1;

        if( currentIndex != openIndex )
        {
            if(null == ftDev)
            {
                ftDev = ftdid2xx.openByIndex(DeviceUARTContext, openIndex);
            }
            else
            {
                synchronized(ftDev)
                {
                    ftDev = ftdid2xx.openByIndex(DeviceUARTContext, openIndex);
                }
            }
            uart_configured = false;
        }
        else
        {
            Toast.makeText(DeviceUARTContext,"Device port " + tmpProtNumber + " is already opened",Toast.LENGTH_LONG).show();
            return;
        }

        if(ftDev == null)
        {
            Toast.makeText(DeviceUARTContext,"open device port("+tmpProtNumber+") NG, ftDev == null", Toast.LENGTH_LONG).show();
            return;
        }

        if (true == ftDev.isOpen())
        {
            currentIndex = openIndex;
            Toast.makeText(DeviceUARTContext, "open device port(" + tmpProtNumber + ") OK", Toast.LENGTH_SHORT).show();

            if(false == bReadThreadGoing)
            {
                //read_thread = new readThread(handler);
                read_thread = new uHer(handler);
                read_thread.ftDev = ftDev;
                read_thread.bReadThreadGoing = false;
                //read_thread.bReadThreadGoing = true;
                //read_thread.start();
                //bReadThreadGoing = true;
            }
        }
        else
        {
            Toast.makeText(DeviceUARTContext, "open device port(" + tmpProtNumber + ") NG", Toast.LENGTH_LONG).show();
            //Toast.makeText(DeviceUARTContext, "Need to get permission!", Toast.LENGTH_SHORT).show();
        }
    }

    public void LogMsg(String msg)
    {
        binding.mShowCount.append(msg+"\n");
    }

    private int getaddr(String respond)
    {
        //String s = new String("P=$05ED00");
        try {
            String[] arr = respond.split("=", 2);
            String s = arr[1].substring(1);
            s = s.replaceAll("(\\r|\\n)", "");
            int ret = Integer.parseInt(s, 16);
            return (ret);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }

    final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (read_thread.iavailable > 0) {
                binding.showCount.setText("");
                binding.showCount.append(read_thread.readString());
                //readText.setText("");
                //readText.append(read_thread.readString());

            }
        }
    };


    public void SetConfig(int baud, byte dataBits, byte stopBits, byte parity, byte flowControl)
    {
        if (ftDev.isOpen() == false) {
            Log.e("j2xx", "SetConfig: device not open");
            return;
        }

        // configure our port
        // reset to UART mode for 232 devices
        ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);

        ftDev.setBaudRate(baud);

        switch (dataBits) {
            case 7:
                dataBits = D2xxManager.FT_DATA_BITS_7;
                break;
            case 8:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
            default:
                dataBits = D2xxManager.FT_DATA_BITS_8;
                break;
        }

        switch (stopBits) {
            case 1:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
            case 2:
                stopBits = D2xxManager.FT_STOP_BITS_2;
                break;
            default:
                stopBits = D2xxManager.FT_STOP_BITS_1;
                break;
        }

        switch (parity) {
            case 0:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
            case 1:
                parity = D2xxManager.FT_PARITY_ODD;
                break;
            case 2:
                parity = D2xxManager.FT_PARITY_EVEN;
                break;
            case 3:
                parity = D2xxManager.FT_PARITY_MARK;
                break;
            case 4:
                parity = D2xxManager.FT_PARITY_SPACE;
                break;
            default:
                parity = D2xxManager.FT_PARITY_NONE;
                break;
        }

        ftDev.setDataCharacteristics(dataBits, stopBits, parity);

        short flowCtrlSetting;
        switch (flowControl) {
            case 0:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
            case 1:
                flowCtrlSetting = D2xxManager.FT_FLOW_RTS_CTS;
                break;
            case 2:
                flowCtrlSetting = D2xxManager.FT_FLOW_DTR_DSR;
                break;
            case 3:
                flowCtrlSetting = D2xxManager.FT_FLOW_XON_XOFF;
                break;
            default:
                flowCtrlSetting = D2xxManager.FT_FLOW_NONE;
                break;
        }

        // TODO : flow ctrl: XOFF/XOM
        // TODO : flow ctrl: XOFF/XOM
        ftDev.setFlowControl(flowCtrlSetting, (byte) 0x0b, (byte) 0x0d);

        uart_configured = true;
        Toast.makeText(DeviceUARTContext, "Config done", Toast.LENGTH_SHORT).show();
    }



    @Override
    public void onResume() {
        super.onResume();
        DevCount = 0;
        createDeviceList();
        if(DevCount > 0)
        {
            connectFunction();
            int baudRate = 500000;
            byte dataBit = 8;
            byte parity = 0;
            byte stopBit =0;
            byte flowControl = 0;

            SetConfig(baudRate, dataBit, stopBit, parity, flowControl);
        }
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public File getAlbumStorageDir(String albumName) {
    // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), albumName);

        if (!file.mkdirs()) {
            Log.e(LOG_TAG, "Directory not created");
        }
        return file;
    }

    private void SaveExample(String ADir, String AFileName ){
        // zkontroluje opravneni a existenci adresare
        File file = getAlbumStorageDir(ADir);
        if(!file.isDirectory())
            file.mkdirs();
        String path = file.toString();

        String string = "Hello world!";
        File myExternalFile = new File(path+File.separator+AFileName);
        try {
            //FileOutputStream fos = new FileOutputStream(myExternalFile);
            FileOutputStream fos = new FileOutputStream(myExternalFile);
            fos.write(string.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonToast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // NavHostFragment.findNavController(FirstFragment.this)
               //         .navigate(R.id.action_FirstFragment_to_SecondFragment);

                //SaveExample("test","soubor.csv");
                download(view);
                //showgraph(view);
            }
        });
    }

    /*
    interface OnGeekEventListener {

        // this can be any type of method
        void onGeekEvent(TMereni mer);
    }
     */


    public void showgraph(View v){
        GraphView graphView;
        // on below line we are initializing our graph view.
        //graphView = findViewById(R.id.idGraphView);
        graphView = binding.idGraphView;

        // on below line we are adding data to our graph view.
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[]{
                // on below line we are adding
                // each point on our x and y axis.
                new DataPoint(0, 1),
                new DataPoint(1, 3),
                new DataPoint(2, 4),
                new DataPoint(3, 9),
                new DataPoint(4, 6),
                new DataPoint(5, 3),
                new DataPoint(6, 6),
                new DataPoint(7, 1),
                new DataPoint(8, 2)
        });



        // after adding data to our line graph series.
        // on below line we are setting
        // title for our graph view.
        graphView.setTitle("My Graph View");

        // on below line we are setting
        // text color to our graph view.
        graphView.setTitleColor(R.color.purple_200);

        // on below line we are setting
        // our title text size.
        graphView.setTitleTextSize(18);

        // on below line we are adding
        // data series to our graph view.
        graphView.addSeries(series);
    }



    public void DoGeek(TMereni mer)
    {

    }

    public void download(View v) {

        //DataPoint[] values;
        ArrayList<DataPoint> arl = new ArrayList();
        pars sc = new pars(mer->
        {
            if (mer.dtm != null) {
                DataPoint vx = new DataPoint(mer.dtm, mer.t1);
                arl.add(vx);
            }
        });


        if (ftDev == null)
        {
            Log.e("j2xx","SendMessage : d2xx device doesnt exist");
            LogMsg("Device doesnt exist. Check USB port");
            return;
        }

        if (ftDev.isOpen() == false) {
            Log.e("j2xx", "SendMessage: device not open");
            return;
        }
        read_thread.prepcom(); //rts/dtr on/off
        byte[] b = new byte[5];
        //binding.showCount.setText("");

        String adapter = read_thread.getAdapter();
        if (adapter.isEmpty()) {
            LogMsg("No adapter present");
            Toast.makeText(getActivity(), "Please attach lolly device!",
                    Toast.LENGTH_LONG).show();
            return;
        }
        String respond = read_thread.doCommand("#");
        if (respond.isEmpty()) {
            LogMsg("Please attach lolly device");
            return;
        }

        progressBarStatus = 0;
        //  fileSize = 0;
        Log.d("Sendmessage", respond);
        // writeButton.setText(respond);
        respond = read_thread.doCommand(" ");
        LogMsg(respond);
        Log.d("Sendmessage", respond);
        respond = read_thread.doCommand("S=$000000");
       // LogMsg(respond);
        Log.d("Sendmessage", respond);

        // napocitej pocet cyklu z posledni adresy
        respond = read_thread.doCommand("P");
        int lastAddress = getaddr(respond);
        //progressBar.setMax(lastAddress);
        binding.proBar.setMax(lastAddress);

        fAddr = 0;
        // com.ftdi.javad2xxdemo.pars sc = new com.ftdi.javad2xxdemo.pars();


        new Thread(new Runnable() {
            private String respond = "";
            public void run() {

                try {
                    // storage/emulated/0/Documents
                    File file= new File ("/sdcard/Documents/dimage.txt");
                    FileWriter fwimg;
                    if (file.exists())
                        fwimg = new FileWriter(file,false);//if file exists append to file. Works fine
                    else
                    {
                        file.createNewFile();
                        fwimg = new FileWriter(file);
                    }

                    File fileimg= new File ("/sdcard/Documents/decoded.txt");
                    FileWriter fwdec;
                    if (file.exists())
                        fwdec = new FileWriter(fileimg,false);//if file exists append to file. Works fine
                    else
                    {
                        fileimg.createNewFile();
                        fwdec = new FileWriter(fileimg);
                    }

                    String ss = "";
                    while (progressBarStatus < lastAddress) {
                        // performing operation
                        respond = read_thread.doCommand("D");
                        fwimg.write(respond);

                        // einschrotter
                        ss = sc.dpacket(respond);
                        fwdec.write(ss);
                        // LogMsg(ss);

                        // jakou mam aktualne adresu
                        respond = read_thread.doCommand("S");
                        fwimg.write(respond);

                        fAddr = getaddr(respond);
                        progressBarStatus = fAddr;

                        // Updating the progress bar
                        progressBarHandler.post(new Runnable() {
                            public void run() {
                                //progressBar.setProgress(progressBarStatus);
                                binding.proBar.setProgress(progressBarStatus);
                            }
                        });
                    }
                    fwimg.flush();
                    fwimg.close();

                    fwdec.flush();
                    fwdec.close();
                    //LogMsg(file.toString());

                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }


                String s = String.format("Read %s measurements",arl.size());
                for (DataPoint p: arl)
                {
                    System.out.println(p.getX()+" "+p.getY());
                }

                DataPoint[] values = new DataPoint[arl.size()];
                for (DataPoint p: arl){
                    DataPoint v = new DataPoint(p.getX(),p.getY());
                }

                GraphView graphView = binding.idGraphView;
                graphView.addSeries(new LineGraphSeries(values));


                /*
                LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(new DataPoint[]{
                        for (DataPoint val: values)
                            System.out.println(val.getX()+" "+val.getY());
                        new DataPoint(0, 1),
                        new DataPoint(1, 3),
                        new DataPoint(2, 4),
                        new DataPoint(3, 9),
                        new DataPoint(4, 6),
                        new DataPoint(5, 3),
                        new DataPoint(6, 6),
                        new DataPoint(7, 1),
                        new DataPoint(8, 2)

                });
                */


                // performing operation if file is downloaded,
                if (progressBarStatus >= lastAddress) {
                    // sleeping for 1 second after operation completed
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // close the progress bar dialog
                    //progressBar.dismiss();
                }
            }
        }).start();
    }

    @Override
    public void onStart() {
        super.onStart();
        createDeviceList();
    }

    public void createDeviceList()
    {

        DeviceUARTContext = getContext();

        int tempDevCount = ftdid2xx.createDeviceInfoList(DeviceUARTContext);
        if (tempDevCount > 0)
        {
            if( DevCount != tempDevCount )
            {
                DevCount = tempDevCount;
                //updatePortNumberSelector();
            }
        }
        else
        {
            DevCount = -1;
            currentIndex = -1;
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStop()
    {
        disconnectFunction();
        super.onStop();
    }


    public void showToast(View view) {
        Toast toast = Toast.makeText(getContext(),R.string.toast_message, Toast.LENGTH_SHORT);

        toast.show();
    }

    public void countUp(View view) {
        if (binding.showCount != null) {
            binding.showCount.setText(Integer.toString(mCount));
        }
        mCount++;
    }


}