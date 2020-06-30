package net.gudenau.minecraft.fps.fixes;

import com.mojang.datafixers.Dynamic;
import com.mojang.datafixers.types.DynamicOps;
import it.unimi.dsi.fastutil.longs.LongIterable;
import it.unimi.dsi.fastutil.longs.LongIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.StreamSupport;
import net.gudenau.minecraft.fps.util.AssemblyTarget;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.CuboidBlockIterator;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Position;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

@AssemblyTarget
public class BlockPosFixes{
    private static final int BITS_X = 26;
    private static final int BITS_Y = 11;
    private static final int BITS_Z = 26;
    private static final int BITS_I = 1;
    
    private static final long SHIFT_X = BITS_Y + BITS_Z;
    private static final long SHIFT_Y = 0;
    private static final long SHIFT_Z = BITS_Y;
    private static final long SHIFT_I = BITS_Y + BITS_Z + BITS_Y;
    
    private static final long MASK_X = 0x03FFFFFF;
    private static final long MASK_Y = 0x000007FF;
    private static final long MASK_Z = 0x03FFFFFF;
    private static final long MASK_I = 0x00000001;
    
    @AssemblyTarget
    public static boolean isImmutable(long value){
        return ((value >>> SHIFT_I) & MASK_I) == 0;
    }
    
    @AssemblyTarget
    public static long init(int x, int y, int z){
        return ((x & MASK_X) << SHIFT_X) |
               ((y & MASK_Y) << SHIFT_Y) |
               ((z & MASK_Z) << SHIFT_Z);
    }
    
    @AssemblyTarget
    public static long init(double x, double y, double z) {
        return init(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }
    
    @AssemblyTarget
    public static long init(Entity entity){
        return init(entity.getX(), entity.getY(), entity.getZ());
    }
    
    @AssemblyTarget
    public static long init(Vec3d value){
        return init(value.getX(), value.getY(), value.getZ());
    }
    
    @AssemblyTarget
    public static long init(Position pos){
        return init(pos.getX(), pos.getY(), pos.getZ());
    }
    
    @AssemblyTarget
    public static long init(Vec3i pos) {
        return init(pos.getX(), pos.getY(), pos.getZ());
    }
    
    @AssemblyTarget
    public static <T> long deserialize(Dynamic<T> dynamic) {
        Spliterator.OfInt ofInt = dynamic.asIntStream().spliterator();
        int[] is = new int[3];
        if(
            ofInt.tryAdvance((IntConsumer)(i)->is[0] = i) &&
            ofInt.tryAdvance((IntConsumer)(i)->is[1] = i)
        ) {
            ofInt.tryAdvance((IntConsumer)(i)->is[2] = i);
        }
        return init(is[0], is[1], is[2]);
    }
    
    @AssemblyTarget
    public static <T> T serialize(long value, DynamicOps<T> ops) {
        return ops.createIntList(IntStream.of(
            unpackLongX(value),
            unpackLongY(value),
            unpackLongZ(value)
        ));
    }
    
    @AssemblyTarget
    public static long add(long value, int x, int y, int z) {
        return asLong(unpackLongX(value) + x, unpackLongY(value) + y, unpackLongZ(value) + z);
    }
    
    @AssemblyTarget
    public static int unpackLongX(long x){
        return (int)((x >>> SHIFT_X) & MASK_X);
    }
    
    @AssemblyTarget
    public static int unpackLongY(long y){
        return (int)((y >>> SHIFT_Y) & MASK_Y);
    }
    
    @AssemblyTarget
    public static int unpackLongZ(long z){
        return (int)((z >>> SHIFT_Z) & MASK_Z);
    }
    
    @AssemblyTarget
    public static long fromLong(long value) {
        return value;
    }
    
    @AssemblyTarget
    public static long asLong(int x, int y, int z) {
        return init(x, y, z);
    }
    
    @AssemblyTarget
    public static long removeChunkSectionLocalY(long y){
        return y & -16L;
    }
    
    @AssemblyTarget
    public static long asLong(long value){
        return value;
    }
    
    @AssemblyTarget
    public static long add(long value, double x, double y, double z){
        return init(unpackLongX(value) + x, unpackLongY(value) + y, unpackLongZ(value) + z);
    }
    
    @AssemblyTarget
    public static long add(long value, Vec3i pos){
        return add(value, pos.getX(), pos.getY(), pos.getZ());
    }
    
    @AssemblyTarget
    public static long subtract(long value, Vec3i pos){
        return add(value, -pos.getX(), -pos.getY(), -pos.getZ());
    }
    
    @AssemblyTarget
    public static long up(long value){
        return offset(value, Direction.UP);
    }
    
    @AssemblyTarget
    public static long up(long value, int distance) {
        return offset(value, Direction.UP, distance);
    }
    
    @AssemblyTarget
    public static long down(long value){
        return offset(value, Direction.DOWN);
    }
    
    @AssemblyTarget
    public static long down(long value, int distance) {
        return offset(value, Direction.DOWN, distance);
    }
    
    @AssemblyTarget
    public static long north(long value) {
        return offset(value, Direction.NORTH);
    }
    
    @AssemblyTarget
    public static long north(long value, int distance) {
        return offset(value, Direction.NORTH, distance);
    }
    
    @AssemblyTarget
    public static long south(long value) {
        return offset(value, Direction.SOUTH);
    }
    
    @AssemblyTarget
    public static long south(long value, int distance) {
        return offset(value, Direction.SOUTH, distance);
    }
    
    @AssemblyTarget
    public static long west(long value) {
        return offset(value, Direction.WEST);
    }
    
    @AssemblyTarget
    public static long west(long value, int distance) {
        return offset(value, Direction.WEST, distance);
    }
    
    @AssemblyTarget
    public static long east(long value) {
        return offset(value, Direction.EAST);
    }
    
    @AssemblyTarget
    public static long east(long value, int distance) {
        return offset(value, Direction.EAST, distance);
    }
    
    @AssemblyTarget
    public static long offset(long value, Direction direction) {
        return init(
            unpackLongX(value) + direction.getOffsetX(),
            unpackLongY(value) + direction.getOffsetY(),
            unpackLongZ(value) + direction.getOffsetZ()
        );
    }
    
    @AssemblyTarget
    public static long offset(long value, Direction direction, int amount) {
        return amount == 0 ? value : init(
            unpackLongX(value) + direction.getOffsetX() * amount,
            unpackLongY(value) + direction.getOffsetY() * amount,
            unpackLongZ(value) + direction.getOffsetZ() * amount
        );
    }
    
    @AssemblyTarget
    public static long rotate(long value, BlockRotation rotation) {
        switch(rotation) {
            case NONE:
            default:
                return value;
            case CLOCKWISE_90:
                return init(-unpackLongZ(value), unpackLongY(value), unpackLongX(value));
            case CLOCKWISE_180:
                return init(-unpackLongX(value), unpackLongY(value), -unpackLongZ(value));
            case COUNTERCLOCKWISE_90:
                return init(unpackLongZ(value), unpackLongY(value), -unpackLongX(value));
        }
    }
    
    @AssemblyTarget
    public static long crossProduct(long value, Vec3i pos) {
        return init(
            unpackLongY(value) * pos.getZ() - unpackLongZ(value) * pos.getY(),
            unpackLongZ(value) * pos.getX() - unpackLongX(value) * pos.getZ(),
            unpackLongX(value) * pos.getY() - unpackLongY(value) * pos.getX()
        );
    }
    
    //FIXME
    @AssemblyTarget
    public static long toImmutable(long value){
        return value;
    }
    
    @AssemblyTarget
    public static LongIterable iterate(long pos1, long pos2) {
        return iterate(
            Math.min(unpackLongX(pos1), unpackLongX(pos2)),
            Math.min(unpackLongY(pos1), unpackLongY(pos2)),
            Math.min(unpackLongZ(pos1), unpackLongZ(pos2)),
            Math.max(unpackLongX(pos1), unpackLongX(pos2)),
            Math.max(unpackLongY(pos1), unpackLongY(pos2)),
            Math.max(unpackLongZ(pos1), unpackLongZ(pos2))
        );
    }
    
    @AssemblyTarget
    public static LongStream stream(long pos1, long pos2) {
        return stream(
            Math.min(unpackLongX(pos1), unpackLongX(pos2)),
            Math.min(unpackLongY(pos1), unpackLongY(pos2)),
            Math.min(unpackLongZ(pos1), unpackLongZ(pos2)),
            Math.max(unpackLongX(pos1), unpackLongX(pos2)),
            Math.max(unpackLongY(pos1), unpackLongY(pos2)),
            Math.max(unpackLongZ(pos1), unpackLongZ(pos2))
        );
    }
    
    @AssemblyTarget
    public static LongStream method_23627(BlockBox blockBox) {
        return stream(
            Math.min(blockBox.minX, blockBox.maxX),
            Math.min(blockBox.minY, blockBox.maxY),
            Math.min(blockBox.minZ, blockBox.maxZ),
            Math.max(blockBox.minX, blockBox.maxX),
            Math.max(blockBox.minY, blockBox.maxY),
            Math.max(blockBox.minZ, blockBox.maxZ)
        );
    }
    
    @AssemblyTarget
    public static LongStream stream(final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ){
        return StreamSupport.longStream(
            new Spliterators.AbstractLongSpliterator(
                (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1),
                Spliterator.SIZED
            ){
                final CuboidBlockIterator connector = new CuboidBlockIterator(minX, minY, minZ, maxX, maxY, maxZ);
    
                public boolean tryAdvance(LongConsumer consumer){
                    if(connector.step()){
                        consumer.accept(init(connector.getX(), connector.getY(), connector.getZ()));
                        return true;
                    }else{
                        return false;
                    }
                }
            }, false);
    }
    
    @AssemblyTarget
    public static LongIterable iterate(int minX, int maxX, int minY, int maxY, int minZ, int maxZ){
        return ()->new LongIterator(){
            final CuboidBlockIterator iterator = new CuboidBlockIterator(minX, minY, minZ, maxX, maxY, maxZ);
            
            @Override
            public boolean hasNext(){
                return iterator.step();
            }
    
            @Override
            public long nextLong(){
                return init(iterator.getX(), iterator.getY(), iterator.getZ());
            }
        };
    }
}
