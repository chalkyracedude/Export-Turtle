package com.exportturtle.selfie;

import static org.junit.Assert.*;

import org.junit.Test;

public class RotationAngleTest
{
    @Test public void halfTurnSweepsAndReturnsWithoutJumping()
    {
        assertEquals(0, PortraitRenderer.rotationAngle(0, true), 0.00001);
        assertEquals(Math.PI / 2, PortraitRenderer.rotationAngle(18, true), 0.00001);
        assertEquals(-Math.PI / 2, PortraitRenderer.rotationAngle(54, true), 0.00001);
        assertEquals(0, PortraitRenderer.rotationAngle(72, true), 0.00001);
        assertEquals(Math.PI, PortraitRenderer.rotationAngle(36, false), 0.00001);
    }
}
