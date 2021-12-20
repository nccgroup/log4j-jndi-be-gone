/*
Copyright 2021 NCC Group

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package trust.nccgroup.jnditest.test;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.Socket;

import java.lang.reflect.*;
//import agent.org.apache.logging.log4j.Logger;
//import agent.org.apache.logging.log4j.LogManager;

public class JndiTest {
  public static boolean failed = false;

  @Test
  public void logging() throws Throwable {
    Thread t = new Thread() {
      public void run() {
        try {
          InetAddress host = InetAddress.getByName("127.0.0.1");
          ServerSocket serverSocket = new ServerSocket(8899, 1, host);
          Socket clientSocket = serverSocket.accept();
          failed = true;
          clientSocket.close();
        } catch (IOException ioe) {
          ioe.printStackTrace();
          return;
        }
      }
    };
    t.start();

    Class<?> AgentMain = Class.forName("co.elastic.apm.agent.premain.AgentMain");
    Field agentClassLoader_f = AgentMain.getDeclaredField("agentClassLoader");
    agentClassLoader_f.setAccessible(true);
    ClassLoader agentClassLoader = (ClassLoader)agentClassLoader_f.get(null);

    //Logger LOGGER = LogManager.getLogger(JndiTest.class);
    //Class Logger = Class.forName("agent.org.apache.logging.log4j.LogManager");
    Class<?> Logger = agentClassLoader.loadClass("org.apache.logging.log4j.LogManager");
    Method getLogger = Logger.getDeclaredMethod("getLogger", Class.class);
    Object LOGGER = getLogger.invoke(null, JndiTest.class);
    //Class Marker = Class.forName("agent.org.apache.logging.log4j.Marker");
    //Class<?> Marker = agentClassLoader.loadClass("org.apache.logging.log4j.Marker");

    /*
    Method error = null;
    for (Method m : LOGGER.getClass().getSuperclass().getDeclaredMethods()) {
      if (m.getName().equals("error")) {
        Class<?>[] params = m.getParameterTypes();
        if (params.length == 3) {
          if (params[1] == String.class && params[2] == Object.class) {
            System.out.println("m: " + m);
            error = m;
          }
        }
      }
    }*/

    Method error = LOGGER.getClass().getSuperclass().getDeclaredMethod("error", String.class, Object.class);

    //LOGGER.error("Hello, {}!", "_${jndi:ldap://127.0.0.1:8899/evil}_");
    error.invoke(LOGGER, "Hello, {}!", "_${jndi:ldap://127.0.0.1:8899/evil}_");
    if (failed) {
      fail("jndi ldap connection received");
    } else {
      if (t.isAlive()) {
        t.interrupt();
      }
    }
  }

}
