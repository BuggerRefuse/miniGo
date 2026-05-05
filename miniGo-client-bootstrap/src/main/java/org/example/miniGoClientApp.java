package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Hello world!
 *
 */
@SpringBootApplication
//@EnableDiscoveryClient
public class miniGoClientApp
{
    public static void main( String[] args )
    {
        SpringApplication.run(miniGoClientApp.class, args);
        System.out.println( "----Lord,welcome to miniGo!----" );
    }
}
