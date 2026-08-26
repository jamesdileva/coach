package com.coach.plugin.trigger;

/**
 * Rising-edge detector for threshold-style triggers: fires only on the
 * transition from unsatisfied to satisfied, not continuously while satisfied
 * (roadmap Sprint 6: "HP triggers fire when HP crosses threshold").
 */
public final class EdgeDetector
{
	private Boolean last;

	/**
	 * @param current whether the predicate is currently satisfied
	 * @return true only when the predicate just became true
	 */
	public boolean onNext(boolean current)
	{
		boolean fire = current && !Boolean.TRUE.equals(last);
		last = current;
		return fire;
	}

	public void reset()
	{
		last = null;
	}
}
