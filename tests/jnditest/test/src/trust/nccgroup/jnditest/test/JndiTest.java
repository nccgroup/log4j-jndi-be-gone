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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class JndiTest {
  public static boolean failed = false;

  @Test
  public void logging() {
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

    Logger LOGGER = LogManager.getLogger(JndiTest.class);

    LOGGER.error("Hello, {}!", "_${jndi:ldap://127.0.0.1:8899/evil}_");
    if (failed) {
      fail("jndi ldap connection received");
    } else {
      if (t.isAlive()) {
        t.interrupt();
      }
    }
  }

}
