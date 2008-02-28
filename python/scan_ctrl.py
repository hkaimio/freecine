print "importing"
import blueNXT
print "dogtail"
from dogtail.tree import *
print "procedural"
from dogtail.procedural import *
from dogtail.rawinput import *
import time

print "imports"


class FilmMover:

    def __init__(self ):
        self.connection = blueNXT.Blue( 0 )

    def move_film( self ):
	print "Moving film"
        self.connection.put( "scan" )
        time.sleep( 10 )
	print "done"

print "mover"
        
# sd
class Vuescan( Application ):

    def __init__(self ):
        print "Starting scanner"
        Application.__init__( self, root.application( "vuescan" ) )
        self.scan_btn = self.findChild( predicate.IsAButtonNamed( "Scan" ) )
        self.abort_btn = self.findChild( predicate.IsAButtonNamed( "Abort" ) )

    def scan( self ):
	focus.application( 'vuescan' )
	self.scan_btn.click()
        time.sleep(1)
	relativeMotion( 100, 100 )
	time.sleep( 1 )
	absoluteMotion( 100, 30 )
	time.sleep( 1 )
	focus.application( "evolution" )
	time.sleep( 1 )
	focus.application( "vuescan" )
        print "started scan"
        time.sleep( 10 )
        while self.scan_btn.showing < 1:
            print "sleeping..."
            time.sleep( 10 )
        print "scan ready"

print "starting..."
vuescan = Vuescan()
mover = FilmMover()
print "intialized"
# mover.move_film()

while True:
    vuescan.scan()
    mover.move_film()

                          


