/*
 * Copyright 1999-2006 University of Chicago
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.globus.usage.receiver.samples;

import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketException;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.globus.usage.receiver.Receiver;
import org.globus.usage.receiver.handlers.GridFTPPacketHandler;
import org.globus.usage.receiver.handlers.RFTPacketHandler;
import org.globus.usage.receiver.handlers.JavaCorePacketHandler;
import org.globus.usage.receiver.handlers.CCorePacketHandler;
import org.globus.usage.receiver.handlers.GRAMPacketHandler;
import org.globus.usage.receiver.handlers.RLSPacketHandler;

/*An example of how the Receiver class can be used in a program:*/
public class ExampleReceiver {

    static Log log = LogFactory.getLog(ExampleReceiver.class);

    public static void main(String[] args) {
        int port = 0;
        String databaseDriverClass, databaseURL, defaultTable, gftpTable;
	String jwsCoreTable, rlsTable, gramTable, cCoreTable, gftpFilterTable;
	String rftTable, domainsToFilter;
	int ringBufferSize = 0;
        Properties props = new Properties();
        InputStream propsIn;
        Receiver receiver;

        GridFTPPacketHandler gftpHandler;
	RFTPacketHandler rftHandler;
	JavaCorePacketHandler jwsCoreHandler;
	CCorePacketHandler cCoreHandler;
	GRAMPacketHandler gramHandler;
	RLSPacketHandler rlsHandler;

        /*Open properties file (which gets compiled into jar) to read
          default port and database connection information:*/

        String file = "/etc/globus_usage_receiver/receiver.properties";
        propsIn = Receiver.class.getResourceAsStream(file);
        if (propsIn == null) {
            System.err.println("Can't open properties file: " + file);
            System.exit(1);
        }

        databaseDriverClass = props.getProperty("database-driver");
        databaseURL = props.getProperty("database-url");
        defaultTable = props.getProperty("default-table");
        gftpTable = props.getProperty("gftp-table");
        gftpFilterTable = props.getProperty("gftp-filtered-out-table");
        domainsToFilter = props.getProperty("gftp-filter-domains");
        rftTable = props.getProperty("rft-table");
        jwsCoreTable = props.getProperty("jws-core-table");
        cCoreTable = props.getProperty("cws-core-table");
        gramTable = props.getProperty("gram-table");
        rlsTable = props.getProperty("rls-table");
            
        ringBufferSize = Integer.parseInt(props.getProperty("ringbuffer-size"));

        if (args.length == 1)
            /*Get listening port number from command line*/
            port = Integer.parseInt(args[0]);
        else {
            /*or else, read port from properties file:*/
            port = Integer.parseInt(props.getProperty("listening-port"));
        }
        
        if (port == 0) {
            System.err.println("You must specify listening port either on the command line or in the properties file.");
            System.exit(2);
        }
            
        /*When creating the receiver, pass it the port to listen on,
          the database connection class to use, the url to connect to your
          database, and the database table where default packets will be
          written if no other handler takes them:*/
        System.out.println("Starting receiver on port "+port+"; will write to database at "+databaseURL+"; Ringbuffer size is "+ringBufferSize);

        try {
            receiver = new Receiver(port, ringBufferSize, props);
            
            /*gftpHandler is an example of a PacketHandler subclass.  I create
              one here, giving it the neccessary database information, and then
              register it to the receiver; it knows what to do with all
	      incoming GFTP usage packets.*/
            gftpHandler = new GridFTPPacketHandler(databaseURL, gftpFilterTable,
                                                   gftpTable, domainsToFilter);
            receiver.registerHandler(gftpHandler);
	    
	    /*Let's handle RFT usage packets too.  All packets that aren't
	      GFTP or RFT will end up in the unknown_packets table.*/
	    rftHandler = new RFTPacketHandler(databaseURL, rftTable);
	    receiver.registerHandler(rftHandler);
	    jwsCoreHandler = new JavaCorePacketHandler(databaseURL, jwsCoreTable);
	    receiver.registerHandler(jwsCoreHandler);
	    cCoreHandler = new CCorePacketHandler(databaseURL, cCoreTable);
	    receiver.registerHandler(cCoreHandler);
	    gramHandler = new GRAMPacketHandler(databaseURL, gramTable);
	    receiver.registerHandler(gramHandler);
	    rlsHandler = new RLSPacketHandler(databaseURL, rlsTable);;
	    receiver.registerHandler(rlsHandler);

	    //start the control socket thread:
            int controlPort = 4811;
	    new ControlSocketThread(receiver, controlPort).start();
            System.out.println("Starting control socket at: "  + controlPort);
        }
        catch (IOException e) {
            log.fatal("An IOException occurred when trying to create Receiver:" +e.getMessage());
        }
        catch (Exception e) {
            log.fatal("An exception occurred: " + e.getMessage(), e);
        }

        /*That's all... this thread ends, but the receiver has started listener
          and handler threads which will write incoming packets to the database.*/
    }
}

/*Thread used for interprocess communication, so that the receiver can be
  started/stopped/monitored remotely.  Accepts TCP socket connections on the control port.*/
class ControlSocketThread extends Thread {

    private ServerSocket serverSocket;
    private Socket clientSocket;
    private boolean shutDown;
    private Receiver receiver;
    private int controlPort;

    static Log log = LogFactory.getLog(ControlSocketThread.class);

    public ControlSocketThread (Receiver receiver, int controlPort) throws SocketException {
	this.receiver = receiver;
	this.controlPort = controlPort;
	this.shutDown = false;

	try {
	    serverSocket = new ServerSocket(controlPort);
	} catch (IOException e) {
	    log.error("Couldn't open server socket on port "+controlPort);
	}
    }

    public void run() {
	/*When we get a connection on the control socket, either respond with
	  the receiver.getStatus(), or shut down the receiver.*/

	PrintWriter out;
	BufferedReader in;
	String inputLine, outputLine;

	while (!shutDown) {
	    try {
		clientSocket = serverSocket.accept();
		out = new PrintWriter(clientSocket.getOutputStream(), true);
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                inputLine = new String(in.readLine());
                if (inputLine.equals("check")) {
                    out.println(receiver.getStatus(false));
                }
                if (inputLine.equals("clear")) {
                    out.println(receiver.getStatus(true));
                }
                if (inputLine.equals("stop")) {
                    allShutDown();
                }
		out.close();
	    } catch (IOException e) {
		log.error("Accept failed on port " + controlPort);
	    }
	}

	try {
	    clientSocket.close();
	    serverSocket.close();
	} catch (Exception e) {}
    }

    /*Shuts down both this thread and the receiver.*/
    public void allShutDown() {
	this.shutDown = true;
	receiver.shutDown();
    }
    
}
