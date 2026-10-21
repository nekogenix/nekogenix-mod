package io.github.nekogenix.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class CatBodyPose {
    private final ModelPart head, body, frontLeft, frontRight, hindLeft, hindRight, tail, tip;
    private final ModelPart[] arms, legs;
    private final Quaternionf rotation = new Quaternionf();
    private final Vector3f joint = new Vector3f();

    public CatBodyPose(ModelPart root) {
        head = root.getChild("head"); body = root.getChild("body");
        frontLeft = root.getChild("left_front_leg"); frontRight = root.getChild("right_front_leg");
        hindLeft = root.getChild("left_hind_leg"); hindRight = root.getChild("right_hind_leg");
        tail = root.getChild("tail1"); tip = root.getChild("tail2");
        arms = new ModelPart[] {frontLeft, frontRight}; legs = new ModelPart[] {hindLeft, hindRight};
    }

    public void upright(boolean crouching) {
        head.y = crouching ? -1.8F : -8.8F;
        head.z = -1.65F;
        body.xRot = 0;
        body.yScale = 1.1F;
        body.y = head.y - 1.55F;
        body.z = head.z + 7;
        for (var leg : legs) {
            leg.yScale = (24 - (body.y + 19 * body.yScale - .2F)) / 6;
        }
        for (var arm : arms) {
            arm.xRot *= .45F;
        }
        attachUpright();
        attachUprightTail();
    }

    public void attachUpright() {
        for (var arm : arms) anchor(arm, arm == frontLeft ? 2.55F : -2.55F, head.y + 6, head.z + 2.2F, 1);
        for (var leg : legs) anchor(leg, leg == hindLeft ? 1.1F : -1.1F, body.y + 19 * body.yScale - .2F, .2F, 2);
    }

    public void attachUprightTail() {
        anchor(tail, 0, body.y + 17 * body.yScale, body.z - 2.4F, .5F);
        joint.set(0, 8, .5F).rotate(rotation.rotationZYX(tail.zRot, tail.yRot, tail.xRot)).add(tail.x, tail.y, tail.z);
        anchor(tip, joint.x, joint.y, joint.z, .5F);
    }

    private void anchor(ModelPart part, float x, float y, float z, float localZ) {
        joint.set(0, 0, localZ * part.zScale).rotate(rotation.rotationZYX(part.zRot, part.yRot, part.xRot));
        part.setPos(x - joint.x, y - joint.y, z - joint.z);
    }

    public void sitting() {
        body.xRot = Mth.PI / 4; body.y = 8; body.z = -5;
        head.y = 11.7F; head.z = -8;
        tail.y = 23; tail.z = 6; tail.xRot = 1.7278761F;
        tip.y = 22; tip.z = 13.2F; tip.xRot = 2.670354F;
        for (var arm : arms) {
            arm.xRot = -Mth.PI / 20; arm.y = 16.1F; arm.z = -7;
        }
        for (var leg : legs) {
            leg.xRot = -Mth.HALF_PI; leg.y = 21; leg.z = 1;
        }
    }
}
