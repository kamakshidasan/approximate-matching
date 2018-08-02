package matching;

import distmat.DistMatrix;

public class  StblMarriage extends MatchingAlgo {
	
	public StblMarriage() {
		super();
	}
	
	public static boolean DEBUG = false;
	
    public static PersonList getWomenList(int[] prefList, Woman[] women) {
		int pos = prefList[prefList.length - 1];    		
    	PersonList person = new PersonList(women[pos], null);
    	for (int i = prefList.length - 2; i >= 0; i--) {
    		pos = prefList[i];    		
    		person = new PersonList(women[pos], person);
    	}
    	return person;
    }

    public static PersonList getMenList(int[] prefList, Man[] men) {
		int pos = prefList[prefList.length - 1];    		
    	PersonList person = new PersonList(men[pos], null);
    	for (int i = prefList.length - 2; i >= 0; i--) {
    		pos = prefList[i];    		
    		person = new PersonList(men[pos], person);
    	}
    	return person;
    }
	
	@Override
	public Match[] match(DistMatrix dm) {
	   	Man[] men = new Man[dm.getRowNum()];
    	for (int row = 0; row < dm.getRowNum(); row++) {
    		men[row] = new Man(row);
    	}

    	Woman[] women = new Woman[dm.getColNum()];
    	for (int col = 0; col < dm.getColNum(); col++) {
    		women[col] = new Woman(col, dm.getIdCol(col));
    	}
    	
    	for (int row = 0; row < dm.getRowNum(); row++) {
    		int[] pl = dm.getRowPrefs(row);
    		men[row].list = getWomenList(pl, women); 
    	}

    	for (int col = 0; col < dm.getColNum(); col++) {
    		int[] pl = dm.getColPrefs(col);
    		women[col].list = getMenList(pl, men); 
    	}
    	
    	PersonList eligible = new PersonList(men[0], null);
    	for (int row = 1; row < dm.getRowNum(); row++) {
    		eligible = new PersonList(men[row], eligible);
    	}

    	Relation r = findMarriages(eligible);
    	
    	Association cur = r.list;
    	Match[] matches = new Match[Math.min(dm.getRowNum(), dm.getColNum())];    	    	
    	int row = 0;
    	while ((cur != null) && (cur.next != null)) {
    		cur = cur.next;
    		int pos1 = ((Man)cur.range).pos;
    		int pos2 = ((Woman)cur.domain).pos;
    		matches[row] = new Match(dm.getIdRow(pos1), dm.getIdCol(pos2));
    		row++;
    	}	
		return matches;
	}
	
    public static Relation findMarriages(PersonList eligible) {
        Relation couples = new Relation();
        while (eligible != null) {
            Man m = (Man) eligible.person;
            Woman w = m.topPick();
            if (w == null) {
    			eligible = eligible.next;
    			if (DEBUG) {
					System.out.println("No woman left for " + m);
				}
            } else {
            	if (DEBUG) {
					System.out.println("" + m + " proposes to " + w);
				}
            	if (w.likes(m)) {
                	if (DEBUG) {
						System.out.print("  she accepts ");
					}
            		Man oldHusband = (Man) couples.lookup(w);
            		if (oldHusband == null) {
						eligible = eligible.next;
					} else {
                    	if (DEBUG) {
							System.out.print("(dumping " + oldHusband + ")");
						}
            			eligible = new PersonList(oldHusband, eligible.next);
            		}
            		couples.map(w,m);
            		w.trimList(m);
                	if (DEBUG) {
						System.out.println("\n  but she still prefers: " + w.getList());
					}
            	}
            	m.scratchTop();
            }
        }
        return couples;
    }
    
 }

class Man {
	public int pos;
    public PersonList list;
    public Man(int pos) {
        this.pos = pos;
        list = null;
    }
    public Man prepend(Woman w) {
        list = new PersonList(w, list);
        return this;
    }
    public Woman topPick() {
    	if (list != null) {
    		return (Woman) list.person;
    	} else {
    		return null;
    	}
    }
    public void scratchTop() { 
    	list = list.next;
    }
    public String getList() {
        return list.toString();
    }
    @Override
	public String toString() {
        return "" + pos;
    }
}

class Association {
	  public Object domain;
	  public Object range;
	  public Association next;
	  
	  public Association(Object d, Object r, Association next) {
	    domain = d;
	    range = r;
	    this.next = next;
	  }
	  
	  @Override
	public String toString() {
	    return ( "(" + domain + "," + range + ")" );
	  }
	}

class Relation {
	  public Association list;

	  public Relation() {
	    list = new Association(null, null, null);
	  }

	  Association find(Object target) {
	    // return Assocation _before_ target in list
	    Association cur = list;
	    while ((cur.next != null) && (cur.next.domain != target)) {
			cur = cur.next;
		}
	    return cur;
	  }

	  public void map(Object d, Object r) {
	    Association oneBefore = find(d);
	    if (oneBefore.next == null) {
			// didn't find it, so append new item to list end
			  oneBefore.next = new Association(d, r, null);
		} else {
			// did find it, so replace range
			  oneBefore.next.range = r;
		}
	  }

	  public Object drop(Object d) {
	    Association oneBefore = find(d);
	    // make sure not at end of list (if it wasn't found, no need to drop)
	    Object result;
	    if (oneBefore.next != null) {
	      result = oneBefore.next.range;
	      oneBefore.next = oneBefore.next.next;
	      return result;
	    } else {
			return null;
		}
	  }

	  public Object lookup(Object d) {
	    Association oneBefore = find(d);
	    if (oneBefore.next == null) {
			// didn't find it, return null
			  return null;
		} else {
			return oneBefore.next.range;
		}
	  }

	  @Override
	public String toString() {
	    String result = "(";
	    Association cur = list;
	    while (cur.next != null) {
	       result = result + cur.next;
	       cur = cur.next;
	    }
	    return result + ")";
	  }
	} 

class PersonList {
    public Object person;
    public PersonList next;
    public PersonList(Object p, PersonList next) {
        person = p;
        this.next = next;
    }
    @Override
	public String toString() {
        if (next == null) {
			return person.toString();
		} else {
			return ("" + person.toString() + " " + next);
		}
    }
}

class Woman {
    public int pos;
    public PersonList list;
    public Woman(int pos, int id) {
    	this.pos = pos;
        list = null;
    }
    public Woman prepend(Man w) {
        list = new PersonList(w, list);
        return this;
    }
    public boolean likes(Man m) {
        PersonList ls = list;
        while ((ls != null) && (ls.person != m)) {
			ls = ls.next;
		}
        return (ls != null);
    }
    public void trimList(Man m) {  // assumes list contains m
        if (list.person == m) {
			list = null;
		} else {
            PersonList ls = list;
            while (ls.next.person != m) {
				ls = ls.next;
			}
            ls.next = null;       // drop m and all below him
        }
    }
    public String getList() {
        if (list == null) {
			return "nobody";
		} else {
			return list.toString();
		}
    }
    @Override
	public String toString() {
        return "" + pos;
    }
}

