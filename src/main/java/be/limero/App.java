package be.limero;

import akka.actor.ActorSystem;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {

        System.out.println( "Hello World!!" );
        ActorSystem system=ActorSystem.create("actor-system");
    }
}
