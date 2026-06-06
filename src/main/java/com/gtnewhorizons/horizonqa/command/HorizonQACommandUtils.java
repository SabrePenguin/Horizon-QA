package com.gtnewhorizons.horizonqa.command;

import java.util.Collection;

import com.github.bsideup.jabel.Desugar;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public final class HorizonQACommandUtils {

    private static final double RAY_LENGTH = 64.0;

    private HorizonQACommandUtils() {}

    public static CellRecord findTestContaining(BlockPos pos, Collection<CellRecord> cells) {
        for (CellRecord cell : cells) {
            if (cell.posInsideCell(pos))
                return cell;
        }
        return null;
    }

    public static CellRecord findTestAlongLook(EntityPlayer player, Collection<CellRecord> cells) {
        Vec3d eye = player.getPositionEyes(1);
        Vec3d look = player.getLookVec();
        Vec3d endPos = eye.add(look.x * RAY_LENGTH, look.y * RAY_LENGTH, look.z * RAY_LENGTH);

        for (CellRecord cell : cells) {
            if (rayIntersectsAABB(eye, endPos, cell)) {
                return cell;
            }
        }
        return null;
    }

    public static CellRecord findNearestTest(BlockPos pos, Collection<CellRecord> cells) {
        CellRecord nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (CellRecord cell : cells) {
            BlockPos min = cell.minPos;
            BlockPos max = cell.maxPos;
            double cx = (min.getX() + max.getX()) * 0.5;
            double cy = (min.getY() + max.getY()) * 0.5;
            double cz = (min.getZ() + max.getZ()) * 0.5;
            double dx = pos.getX() - cx, dy = pos.getY() - cy, dz = pos.getZ() - cz;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = cell;
            }
        }
        return nearest;
    }

    private static boolean rayIntersectsAABB(Vec3d original, Vec3d far, CellRecord cell) {
        double dx = far.x - original.x, dy = far.y - original.y, dz = far.z - original.z;
        double tmin = 0.0, tmax = 1.0;

        BlockPos min = cell.minPos;
        BlockPos max = cell.maxPos;
        if (Math.abs(dx) < 1e-9) {
            if (original.x < min.getX() || original.x > max.getX() + 1.0) return false;
        } else {
            double t1 = (min.getX() - original.x) / dx;
            double t2 = (max.getX() + 1.0 - original.x) / dx;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return false;
        }

        if (Math.abs(dy) < 1e-9) {
            if (original.y < min.getY() || original.y > max.getY() + 1.0) return false;
        } else {
            double t1 = (min.getY() - original.y) / dy;
            double t2 = (max.getY() + 1.0 - original.y) / dy;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return false;
        }

        if (Math.abs(dz) < 1e-9) {
            if (original.z < min.getZ() || original.z > max.getZ() + 1.0) return false;
        } else {
            double t1 = (min.getZ() - original.z) / dz;
            double t2 = (max.getZ() + 1.0 - original.z) / dz;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tmin = Math.max(tmin, t1);
            tmax = Math.min(tmax, t2);
            if (tmin > tmax) return false;
        }

        return true;
    }

    @Desugar
    public record CellRecord(String testId, BlockPos origin, BlockPos minPos,
                             BlockPos maxPos) {

        public boolean posInsideCell(BlockPos pos) {
            return pos.getX() >= minPos.getX()
                && pos.getY() >= minPos.getY()
                && pos.getZ() >= minPos.getZ()
                && pos.getX() <= maxPos.getX()
                && pos.getY() <= maxPos.getY()
                && pos.getZ() <= maxPos.getZ();
        }
    }
}
