package fpinscala.testing

import org.scalatest.{Matchers, FlatSpec}

/**
 * Created with IntelliJ IDEA.
 * Date: 15-08-22
 * Time: 10:04 PM
 * To change this template use File | Settings | File Templates.
 */
class PropSpec extends FlatSpec with Matchers{
	behavior of "Prop.&&"
	it should "evaluate to true" in {
		val p1 = new Prop {
			override def check: Boolean = true
		}
		val p2 = new Prop {
			override def check: Boolean = true
		}
		(p1 && p2).check should be (true)
	}
	it should "evaluate to false" in {
		val p1 = new Prop {
			override def check: Boolean = true
		}
		val p2 = new Prop {
			override def check: Boolean = false
		}
		(p1 && p2).check should be (false)
	}
	it should "short circuit evaluation" in {
		val p1 = new Prop {
			override def check: Boolean = false
		}
		val p2 = new Prop {
			override def check: Boolean = true
		}
		(p1 && p2).check  should be (false)
	}

}
