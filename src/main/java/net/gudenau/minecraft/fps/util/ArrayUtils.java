package net.gudenau.minecraft.fps.util;

import java.lang.reflect.Array;
import org.objectweb.asm.Type;

/**
 * A few array related things that should be in the STD lib.
 *
 * @author gudenau
 * */
public class ArrayUtils{
    /**
     * Combines a bunch of arrays into a single array.
     *
     * @param arrays The arrays to combine
     *
     * @return The combined array
     * */
    public static int[] combine(int[]... arrays){
        int totalSize = 0;
        for(int[] array : arrays){
            totalSize += array.length;
        }
        int[] result = new int[totalSize];
        int offset = 0;
        for(int[] array : arrays){
            int length = array.length;
            System.arraycopy(array, 0, result, offset, length);
            offset += length;
        }
        return result;
    }
    /**
     * Combines a bunch of arrays into a single array.
     *
     * @param arrays The arrays to combine
     *
     * @return The combined array
     * */
    public static byte[] combine(byte[]... arrays){
        int totalSize = 0;
        for(byte[] array : arrays){
            totalSize += array.length;
        }
        byte[] result = new byte[totalSize];
        int offset = 0;
        for(byte[] array : arrays){
            int length = array.length;
            System.arraycopy(array, 0, result, offset, length);
            offset += length;
        }
        return result;
    }
    
    /**
     * Checks if a value is inside an array.
     *
     * @param array The array to check
     * @param value The value to check for
     *
     * @return True if the array contains the value
     * */
    public static boolean contains(int[] array, int value){
        for(int element : array){
            if(element == value){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Checks if a value is inside an array.
     *
     * @param array The array to check
     * @param value The value to check for
     *
     * @return True if the array contains the value
     * */
    public static <T> boolean contains(T[] array, T value){
        for(T element : array){
            if(element.equals(value)){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Adds an element to the front of an array.
     *
     * @param prefix The element to add
     * @param array The array
     *
     * @return The new array
     * */
    @SuppressWarnings("unchecked")
    public static <T> T[] prefix(T prefix, T[] array){
        T[] result = (T[])Array.newInstance(array.getClass().getComponentType(), array.length + 1);
        result[0] = prefix;
        System.arraycopy(array, 0, result, 1, array.length);
        return result;
    }
}
