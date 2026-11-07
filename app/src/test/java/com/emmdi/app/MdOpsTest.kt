package com.emmdi.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MdOpsTest {
    @Test fun toggle() {
        val md = "# T\n- [ ] uno\n  * [x] due\n> 1. [ ] tre\nniente"
        assertEquals("# T\n- [x] uno\n  * [x] due\n> 1. [ ] tre\nniente", MdOps.toggleTask(md, 1))
        assertEquals("# T\n- [ ] uno\n  * [ ] due\n> 1. [ ] tre\nniente", MdOps.toggleTask(md, 2))
        assertEquals("# T\n- [ ] uno\n  * [x] due\n> 1. [x] tre\nniente", MdOps.toggleTask(md, 3))
        assertEquals(md, MdOps.toggleTask(md, 4))
        assertEquals(md, MdOps.toggleTask(md, 99))
    }

    @Test fun replaceAndDelete() {
        val md = "a\n\nb\nb2\n\nc"
        assertEquals("a\n\nB\n\nc", MdOps.replaceLines(md, 2, 3, "B\n"))
        assertEquals("a\n\nc", MdOps.replaceLines(md, 2, 3, ""))
        assertEquals("b\nb2", MdOps.slice(md, 2, 3))
    }

    @Test fun appendBlock() {
        assertEquals("- [ ] x\n", MdOps.append("", "- [ ] x"))
        assertEquals("a\n\n- [ ] x\n", MdOps.append("a\n\n", "- [ ] x"))
    }
}
