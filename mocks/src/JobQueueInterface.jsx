import { useState, useRef, useEffect } from 'react';
import { Search, Filter, Calendar, Clock, Settings, User, MapPin, ChevronDown, X, Check, Menu } from 'lucide-react';

export default function MobileJobQueueInterface() {
  const [activeIndex, setActiveIndex] = useState(0);
  const [activeFilter, setActiveFilter] = useState('all');
  const [showFilters, setShowFilters] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [swipeDirection, setSwipeDirection] = useState(null);
  const [touchStart, setTouchStart] = useState(null);
  const [touchEnd, setTouchEnd] = useState(null);
  const cardRef = useRef(null);
  
  // Technician profile
  const tech = {
    name: "Michael Rodriguez",
    title: "Senior HVAC Technician",
    id: "TECH-387",
    stats: { total: 5, completed: 1 }
  };

  // Sample job data
  const jobs = [
    {
      id: 'JOB-2025-0431',
      title: 'Emergency AC Repair',
      customer: 'Sunrise Senior Living',
      address: '2200 Eldercare Way',
      priority: 'urgent',
      dueDate: 'Today, 1:00 PM',
      desc: 'AC failure in east wing. Senior residence facility requires immediate attention.',
      time: '2-5 hrs'
    },
    {
      id: 'JOB-2025-0428',
      title: 'HVAC System Repair',
      customer: 'Riverdale Apartments',
      address: '1250 Parkview Ave, Building C',
      priority: 'high',
      dueDate: 'Today, 2:00 PM',
      desc: 'Multiple tenant complaints about insufficient cooling in common areas.',
      time: '3-4 hrs'
    },
    {
      id: 'JOB-2025-0429',
      title: 'Quarterly Maintenance',
      customer: 'Summit Office Complex',
      address: '875 Business Park Drive',
      priority: 'medium',
      dueDate: 'Today, 5:00 PM',
      desc: 'Scheduled quarterly maintenance of all lobby HVAC units.',
      time: '2-3 hrs'
    },
    {
      id: 'JOB-2025-0430',
      title: 'Thermostat Replacement',
      customer: 'Greenfield Residence',
      address: '45 Oakwood Lane',
      priority: 'medium',
      dueDate: 'Tomorrow, 11:00 AM',
      desc: 'Customer reports thermostat unresponsive. Bring replacement units.',
      time: '1 hr'
    },
    {
      id: 'JOB-2025-0432',
      title: 'New System Assessment',
      customer: 'Highland Restaurant',
      address: '890 Culinary Blvd',
      priority: 'low',
      dueDate: 'Tomorrow, 3:00 PM',
      desc: 'Customer interested in upgrading kitchen ventilation system.',
      time: '2 hrs'
    }
  ];

  // Filter and sort jobs
  const priorityOrder = { urgent: 0, high: 1, medium: 2, low: 3 };
  const filteredJobs = activeFilter === 'all' 
    ? [...jobs].sort((a, b) => priorityOrder[a.priority] - priorityOrder[b.priority])
    : [...jobs].filter(job => job.priority === activeFilter)
                .sort((a, b) => priorityOrder[a.priority] - priorityOrder[b.priority]);

  // Handle navigation
  const nextJob = () => {
    setActiveIndex((prevIndex) => (prevIndex + 1) % filteredJobs.length);
    setSwipeDirection('right');
    setTimeout(() => setSwipeDirection(null), 300);
  };

  const prevJob = () => {
    setActiveIndex((prevIndex) => (prevIndex - 1 + filteredJobs.length) % filteredJobs.length);
    setSwipeDirection('left');
    setTimeout(() => setSwipeDirection(null), 300);
  };

  // Touch handlers for swipe
  const handleTouchStart = (e) => {
    setTouchStart(e.targetTouches[0].clientX);
  };
  
  const handleTouchMove = (e) => {
    setTouchEnd(e.targetTouches[0].clientX);
  };
  
  const handleTouchEnd = () => {
    if (!touchStart || !touchEnd) return;
    const distance = touchStart - touchEnd;
    const isLeftSwipe = distance > 50;
    const isRightSwipe = distance < -50;
    
    if (isLeftSwipe) {
      nextJob();
    }
    if (isRightSwipe) {
      prevJob();
    }
    
    // Reset values
    setTouchStart(null);
    setTouchEnd(null);
  };

  // Helper functions for styling
  const getPriorityColor = (priority) => {
    const colors = {
      urgent: 'bg-red-500',
      high: 'bg-orange-500',
      medium: 'bg-blue-500',
      low: 'bg-green-500'
    };
    return colors[priority] || 'bg-gray-500';
  };

  const getPriorityStyle = (priority) => {
    const styles = {
      urgent: 'bg-red-100 text-red-800',
      high: 'bg-orange-100 text-orange-800',
      medium: 'bg-blue-100 text-blue-800',
      low: 'bg-green-100 text-green-800'
    };
    return styles[priority] || 'bg-gray-100 text-gray-800';
  };

  const getCardAnimationClass = () => {
    if (swipeDirection === 'left') {
      return 'animate-slide-left';
    } else if (swipeDirection === 'right') {
      return 'animate-slide-right';
    }
    return '';
  };

  // Effect to add animation classes to global styles
  useEffect(() => {
    const style = document.createElement('style');
    style.innerHTML = `
      @keyframes slideLeft {
        0% { transform: translateX(100%); opacity: 0; }
        100% { transform: translateX(0); opacity: 1; }
      }
      @keyframes slideRight {
        0% { transform: translateX(-100%); opacity: 0; }
        100% { transform: translateX(0); opacity: 1; }
      }
      .animate-slide-left {
        animation: slideLeft 0.3s ease-out forwards;
      }
      .animate-slide-right {
        animation: slideRight 0.3s ease-out forwards;
      }
    `;
    document.head.appendChild(style);
  
    return () => {
      document.head.removeChild(style);
    };
  }, []);

  return (
    <div className="bg-gray-100 min-h-screen overflow-hidden">
      {/* Mobile Header - Compact for small screens */}
      <header className="bg-white shadow-sm border-b border-gray-200 sticky top-0 z-20">
        <div className="px-4 py-3">
          <div className="flex justify-between items-center">
            <button 
              onClick={() => setMenuOpen(true)}
              className="p-2 rounded-full text-gray-500 hover:bg-gray-100"
            >
              <Menu size={20} />
            </button>
            
            <div className="flex items-center">
              <div className="flex-shrink-0 h-8 w-8 bg-blue-600 rounded-full flex items-center justify-center text-white">
                <User size={16} />
              </div>
              <div className="ml-2">
                <p className="text-sm font-bold text-gray-900">{tech.name}</p>
                <p className="text-xs text-gray-500">ID: {tech.id}</p>
              </div>
            </div>
            
            <button className="p-2 rounded-full text-gray-500 hover:bg-gray-100">
              <Settings size={20} />
            </button>
          </div>
          
          <div className="flex justify-between items-center mt-3">
            <div className="text-xs font-medium text-gray-500">
              Jobs: {tech.stats.completed}/{tech.stats.total}
            </div>
            <div className="flex space-x-2">
              <button 
                onClick={() => setShowFilters(!showFilters)}
                className="flex items-center px-3 py-1 bg-gray-200 rounded-full text-xs font-medium text-gray-700"
              >
                <Filter size={12} className="mr-1" />
                Filter
                <ChevronDown size={12} className="ml-1" />
              </button>
            </div>
          </div>
        </div>
        
        {/* Expandable Filter Section */}
        {showFilters && (
          <div className="px-4 py-2 bg-gray-50 border-t border-gray-200 transition-all">
            <div className="flex justify-between items-center mb-2">
              <p className="text-xs font-medium text-gray-500">Filter by priority</p>
              <button 
                onClick={() => setShowFilters(false)}
                className="text-gray-400 hover:text-gray-500"
              >
                <X size={16} />
              </button>
            </div>
            <div className="flex flex-wrap gap-2">
              {['all', 'urgent', 'high', 'medium', 'low'].map(filter => (
                <button
                  key={filter}
                  onClick={() => {
                    setActiveFilter(filter);
                    setActiveIndex(0);
                  }}
                  className={`px-3 py-1 rounded-full text-xs font-medium ${
                    activeFilter === filter
                      ? filter === 'all' 
                        ? 'bg-gray-900 text-white' 
                        : `${filter === 'urgent' ? 'bg-red-600' : filter === 'high' ? 'bg-orange-600' : filter === 'medium' ? 'bg-blue-600' : 'bg-green-600'} text-white`
                      : `${filter === 'all' ? 'bg-gray-200 text-gray-700' : getPriorityStyle(filter)}`
                  }`}
                >
                  {filter === 'all' ? 'All Jobs' : filter.charAt(0).toUpperCase() + filter.slice(1)}
                </button>
              ))}
            </div>
          </div>
        )}
      </header>

      {/* Side Menu (Slide out) */}
      <div className={`fixed inset-0 z-50 transition-all duration-300 ${menuOpen ? 'visible' : 'invisible'}`}>
        {/* Backdrop */}
        <div 
          className={`absolute inset-0 bg-gray-900 transition-opacity duration-300 ${menuOpen ? 'opacity-50' : 'opacity-0'}`}
          onClick={() => setMenuOpen(false)}
        ></div>
        
        {/* Menu */}
        <div className={`absolute top-0 left-0 h-full w-64 bg-white shadow-lg transform transition-transform duration-300 ${menuOpen ? 'translate-x-0' : '-translate-x-full'}`}>
          <div className="p-4 border-b border-gray-200">
            <div className="flex items-center">
              <div className="h-10 w-10 bg-blue-600 rounded-full flex items-center justify-center text-white">
                <User size={20} />
              </div>
              <div className="ml-3">
                <h2 className="text-lg font-bold text-gray-900">{tech.name}</h2>
                <p className="text-sm text-gray-500">{tech.title}</p>
              </div>
            </div>
          </div>
          
          <nav className="p-4">
            <ul className="space-y-2">
              <li>
                <a href="#" className="flex items-center px-4 py-2 bg-blue-50 text-blue-700 rounded-md font-medium">
                  <span>Job Queue</span>
                  <span className="ml-auto bg-blue-100 text-blue-800 px-2 py-0.5 rounded-full text-xs">
                    {filteredJobs.length}
                  </span>
                </a>
              </li>
              <li>
                <a href="#" className="flex items-center px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">
                  <span>Completed Jobs</span>
                </a>
              </li>
              <li>
                <a href="#" className="flex items-center px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">
                  <span>Schedule</span>
                </a>
              </li>
              <li>
                <a href="#" className="flex items-center px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">
                  <span>Map View</span>
                </a>
              </li>
              <li>
                <a href="#" className="flex items-center px-4 py-2 text-gray-700 hover:bg-gray-100 rounded-md">
                  <span>My Profile</span>
                </a>
              </li>
            </ul>
          </nav>
          
          <div className="absolute bottom-0 left-0 right-0 p-4 border-t border-gray-200">
            <button 
              onClick={() => setMenuOpen(false)}
              className="w-full py-2 bg-gray-200 text-gray-800 rounded-md font-medium"
            >
              Close Menu
            </button>
          </div>
        </div>
      </div>

      {/* Job Cards Container - Optimized for touch */}
      <div className="px-4 py-4">
        <div className="flex justify-between items-center mb-2">
          <h2 className="text-base font-medium text-gray-900">Active Jobs ({filteredJobs.length})</h2>
          <span className="text-xs text-gray-500">
            {activeIndex + 1} of {filteredJobs.length}
          </span>
        </div>

        {filteredJobs.length > 0 ? (
          <div 
            className="relative min-h-96 mb-4" 
            ref={cardRef}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
          >
            {/* Swipe Hint Overlay (shows only on first render) */}
            <div className="absolute inset-0 flex items-center justify-center z-10 pointer-events-none opacity-30">
              <div className="text-center text-gray-400 animate-pulse">
                <div className="flex justify-center">
                  <span className="mx-2">←</span>
                  <span>Swipe to navigate</span>
                  <span className="mx-2">→</span>
                </div>
              </div>
            </div>
            
            {/* Active Card */}
            {filteredJobs.length > 0 && (
              <div 
                className={`bg-white rounded-xl shadow-lg overflow-hidden ${getCardAnimationClass()}`} 
              >
                {/* Priority Bar */}
                <div className={`h-2 w-full ${getPriorityColor(filteredJobs[activeIndex].priority)}`}></div>
                
                <div className="p-4">
                  <div className="flex justify-between items-start">
                    <div>
                      <div className="flex items-center">
                        <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${getPriorityStyle(filteredJobs[activeIndex].priority)}`}>
                          {filteredJobs[activeIndex].priority.charAt(0).toUpperCase() + filteredJobs[activeIndex].priority.slice(1)}
                        </span>
                        <span className="ml-2 text-xs text-gray-500">
                          {filteredJobs[activeIndex].id}
                        </span>
                      </div>
                      <h3 className="mt-1 text-lg font-bold text-gray-900">{filteredJobs[activeIndex].title}</h3>
                    </div>
                    <div className="flex items-center text-xs text-gray-500">
                      <Clock size={14} className="mr-1 text-gray-400" />
                      {filteredJobs[activeIndex].time}
                    </div>
                  </div>
                  
                  <p className="mt-2 mb-4 text-sm text-gray-600">{filteredJobs[activeIndex].desc}</p>
                  
                  <div className="space-y-3 mb-4">
                    <div className="flex items-start text-sm text-gray-500">
                      <MapPin size={14} className="mr-2 text-gray-400 flex-shrink-0 mt-1" />
                      <div>
                        <div className="font-medium">{filteredJobs[activeIndex].customer}</div>
                        <div className="text-xs">{filteredJobs[activeIndex].address}</div>
                      </div>
                    </div>
                    <div className="flex items-center text-sm text-gray-500">
                      <Calendar size={14} className="mr-2 text-gray-400" />
                      <div>Due: <span className="font-medium">{filteredJobs[activeIndex].dueDate}</span></div>
                    </div>
                  </div>
                  
                  <div className="flex flex-col space-y-2 mt-4">
                    <button className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg">
                      Start Job
                    </button>
                    <button className="w-full py-2 bg-gray-100 hover:bg-gray-200 text-gray-800 text-sm font-medium rounded-lg">
                      Reassign
                    </button>
                  </div>
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="bg-white rounded-xl shadow-lg p-6 text-center">
            <p className="text-gray-600">No jobs match this filter.</p>
          </div>
        )}
        
        {/* Pagination Dots */}
        {filteredJobs.length > 1 && (
          <div className="flex justify-center mb-4 space-x-1">
            {filteredJobs.map((_, index) => (
              <button
                key={index}
                onClick={() => setActiveIndex(index)}
                className={`h-2 w-${index === activeIndex ? '6' : '2'} rounded-full ${
                  index === activeIndex ? 'bg-blue-600' : 'bg-gray-300'
                }`}
                aria-label={`Go to card ${index + 1}`}
              />
            ))}
          </div>
        )}
        
        {/* Swipe Navigation Buttons - Visible on larger phones */}
        {filteredJobs.length > 1 && (
          <div className="flex justify-between items-center px-4 py-2">
            <button 
              onClick={prevJob}
              className="px-4 py-2 bg-gray-200 rounded-lg text-gray-700 flex items-center text-sm"
            >
              <span>←</span>
              <span className="ml-1">Previous</span>
            </button>
            
            <button 
              onClick={nextJob}
              className="px-4 py-2 bg-gray-200 rounded-lg text-gray-700 flex items-center text-sm"
            >
              <span>Next</span>
              <span className="ml-1">→</span>
            </button>
          </div>
        )}
        
        {/* Quick Action Bar */}
        <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-4 py-3 flex justify-around z-10">
          <button className="flex flex-col items-center text-blue-600">
            <MapPin size={20} />
            <span className="text-xs mt-1">Map</span>
          </button>
          <button className="flex flex-col items-center text-gray-500">
            <Calendar size={20} />
            <span className="text-xs mt-1">Schedule</span>
          </button>
          <button className="flex flex-col items-center text-gray-500">
            <Check size={20} />
            <span className="text-xs mt-1">Completed</span>
          </button>
          <button className="flex flex-col items-center text-gray-500">
            <Search size={20} />
            <span className="text-xs mt-1">Search</span>
          </button>
        </div>
      </div>
    </div>
  );
}